using Csjnet.Entidades;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using Newtonsoft.Json;
using System.Data;

namespace Csjnet.Controllers
{
    [ApiController]
    [Route("api/pedido")]
    public class PedidoController : ControllerBase
    {
        private readonly ApplicationDbContext context;

        public PedidoController(ApplicationDbContext context)
        {
            this.context = context;
        }

        [HttpPost]
        [Route("CrearPedidoCompletoHibrido")]
        public async Task<ActionResult> CrearPedidoCompletoHibrido([FromBody] PedidoNuevoOptimizado pedidoNuevo)
        {
            try
            {
                // Configurar timeouts para evitar bloqueos prolongados
                context.Database.SetCommandTimeout(120);
                await context.Database.ExecuteSqlRawAsync("SET LOCK_TIMEOUT 65000");

                // Generar RequestId único para idempotencia
                string requestId = Guid.NewGuid().ToString();

                // Variables para almacenar la información del pedido base
                PedidoBaseInfo pedidoBase = null;
                int itemsProcesados = 0;

                // 1. Crear pedido base (solo cabecera) sin items
                using (var command = context.Database.GetDbConnection().CreateCommand())
                {
                    command.CommandText = "dbo.MARKET_CrearPedidoCompletoCabecera_Optimizado";
                    command.CommandType = CommandType.StoredProcedure;
                    command.CommandTimeout = 120;

                    // Parámetros del stored procedure
                    command.Parameters.Add(new SqlParameter("@IdPersona", pedidoNuevo.Pedido.IdPersona));
                    command.Parameters.Add(new SqlParameter("@IdDireccionEntrega", pedidoNuevo.Pedido.IdDireccionEntrega));
                    command.Parameters.Add(new SqlParameter("@TotalVenta", pedidoNuevo.Pedido.TotalVenta));
                    command.Parameters.Add(new SqlParameter("@Peso", pedidoNuevo.Pedido.Peso));
                    command.Parameters.Add(new SqlParameter("@TipoCp", pedidoNuevo.Pedido.TipoCp));
                    command.Parameters.Add(new SqlParameter("@RequestId", requestId));

                    await context.Database.OpenConnectionAsync();

                    using (var reader = await command.ExecuteReaderAsync())
                    {
                        if (await reader.ReadAsync())
                        {
                            pedidoBase = new PedidoBaseInfo
                            {
                                IdCp = reader.GetInt32(reader.GetOrdinal("IdCp")),
                                IdCpInventario = reader.GetInt32(reader.GetOrdinal("IdCpInventario")),
                                NumCp = reader.GetString(reader.GetOrdinal("NumCp")),
                                Fecha = reader.GetDateTime(reader.GetOrdinal("Fecha")),
                                RequestId = reader.GetString(reader.GetOrdinal("RequestId"))
                            };
                        }
                    }
                }

                if (pedidoBase == null)
                {
                    return BadRequest("No se pudo crear el pedido base");
                }

                // 2. Procesar items uno por uno con transacciones individuales
                foreach (var item in pedidoNuevo.Items)
                {
                    try
                    {
                        using (var itemCommand = context.Database.GetDbConnection().CreateCommand())
                        {
                            itemCommand.CommandText = "dbo.MARKET_InsertarItemPedido_Optimizado";
                            itemCommand.CommandType = CommandType.StoredProcedure;
                            itemCommand.CommandTimeout = 60;

                            // Establecer LOCK_TIMEOUT para evitar bloqueos prolongados
                            using (var timeoutCommand = context.Database.GetDbConnection().CreateCommand())
                            {
                                timeoutCommand.CommandText = "SET LOCK_TIMEOUT 65000";
                                await timeoutCommand.ExecuteNonQueryAsync();
                            }

                            // Parámetros del stored procedure para cada item
                            itemCommand.Parameters.Add(new SqlParameter("@IdCp", pedidoBase.IdCp));
                            itemCommand.Parameters.Add(new SqlParameter("@IdCpInventario", pedidoBase.IdCpInventario));
                            itemCommand.Parameters.Add(new SqlParameter("@IdProducto", item.IdProducto));
                            itemCommand.Parameters.Add(new SqlParameter("@IdUnidad", item.IdUnidad));
                            itemCommand.Parameters.Add(new SqlParameter("@Cantidad", item.Cantidad));
                            itemCommand.Parameters.Add(new SqlParameter("@Peso", item.Peso));
                            itemCommand.Parameters.Add(new SqlParameter("@Precio", item.Precio));
                            itemCommand.Parameters.Add(new SqlParameter("@Total", item.Total));
                            itemCommand.Parameters.Add(new SqlParameter("@Descripcion", item.Descripcion ?? string.Empty));
                            itemCommand.Parameters.Add(new SqlParameter("@RequestId", requestId));
                            itemCommand.Parameters.Add(new SqlParameter("@TieneBono", item.TieneBono ? 1 : 0));

                            await itemCommand.ExecuteNonQueryAsync();
                            itemsProcesados++;
                        }
                    }
                    catch (Exception itemEx)
                    {
                        return BadRequest(new { error = $"Error al procesar el item: {item.Descripcion} - {itemEx.Message}" });
                    }
                }

                // 3. Retornar respuesta exitosa
                var respuesta = new
                {
                    IdCp = pedidoBase.IdCp,
                    IdCpInventario = pedidoBase.IdCpInventario,
                    NumCp = pedidoBase.NumCp,
                    Fecha = pedidoBase.Fecha,
                    RequestId = pedidoBase.RequestId,
                    ItemsProcesados = itemsProcesados,
                    Mensaje = "Pedido creado exitosamente",
                    Exito = true
                };

                return Ok(JsonConvert.SerializeObject(respuesta, Formatting.None));
            }
            catch (Exception ex)
            {
                return BadRequest($"Error al crear pedido: {ex.Message}");
            }
            finally
            {
                // Restaurar timeout por defecto
                context.Database.SetCommandTimeout(null);
            }
        }

        // Mantener los endpoints existentes del archivo archivosAPi.txt
        [HttpPost]
        [Route("CrearPedidoCompleto")]
        public ActionResult CrearPedidoCompleto([FromBody] PedidoNuevoActual pedidoNuevo)
        {
            var resultado = new List<RespuestaPedido>();

            try
            {
                // FIX: Aumentar CommandTimeout a 120 segundos
                context.Database.SetCommandTimeout(120);

                // Configurar timeout de bloqueos en SQL Server
                context.Database.ExecuteSqlRaw("SET LOCK_TIMEOUT 100000");

                // Generar RequestId único para idempotencia
                string requestId = Guid.NewGuid().ToString();

                // Convertir productos a XML
                var productosXml = BuildProductosXml(pedidoNuevo.Items);

                // Llamar al stored procedure
                resultado = context.RespuestaPedido
                    .FromSqlRaw("EXEC MARKET_CrearPedidoCompleto @IdPersona, @IdDireccionEntrega, @TotalVenta, @Peso, @TipoCp, @RequestId, @ProductosXml",
                        new SqlParameter("@IdPersona", pedidoNuevo.Pedido.IdPersona),
                        new SqlParameter("@IdDireccionEntrega", pedidoNuevo.Pedido.IdDireccionEntrega),
                        new SqlParameter("@TotalVenta", pedidoNuevo.Pedido.TotalVenta),
                        new SqlParameter("@Peso", pedidoNuevo.Pedido.Peso),
                        new SqlParameter("@TipoCp", pedidoNuevo.Pedido.TipoCp),
                        new SqlParameter("@RequestId", requestId),
                        new SqlParameter("@ProductosXml", productosXml) { SqlDbType = SqlDbType.Xml })
                    .ToList();

                // Validar que se obtuvo respuesta
                if (resultado == null || resultado.Count == 0)
                {
                    return BadRequest("No se pudo crear el pedido");
                }

                // Devolver respuesta exitosa
                return Ok(JsonConvert.SerializeObject(resultado[0], Formatting.None));
            }
            catch (Exception ex)
            {
                return BadRequest($"Error al crear pedido: {ex.Message}");
            }
            finally
            {
                // Restaurar timeout por defecto (opcional)
                context.Database.SetCommandTimeout(null);
            }
        }

        // HELPER PARA CONVERTIR PRODUCTOS A XML
        private static string BuildProductosXml(IEnumerable<ItemPedido> items)
        {
            var root = new System.Xml.Linq.XElement("Productos");
            foreach (var i in items)
            {
                root.Add(new System.Xml.Linq.XElement("Producto",
                    new System.Xml.Linq.XElement("IdProducto", i.IdProducto),
                    new System.Xml.Linq.XElement("IdUnidad", i.IdUnidad),
                    new System.Xml.Linq.XElement("Cantidad", i.Cantidad),
                    new System.Xml.Linq.XElement("Peso", i.Peso.ToString(CultureInfo.InvariantCulture)),
                    new System.Xml.Linq.XElement("Precio", i.Precio.ToString(CultureInfo.InvariantCulture)),
                    new System.Xml.Linq.XElement("Total", i.Total.ToString(CultureInfo.InvariantCulture)),
                    new System.Xml.Linq.XElement("Descripcion", i.Descripcion ?? string.Empty)
                ));
            }
            return root.ToString(System.Xml.Linq.SaveOptions.DisableFormatting);
        }
    }
}