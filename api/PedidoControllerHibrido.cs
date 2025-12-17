using Csjnet.Entidades;
using MailKit.Net.Smtp;
using MailKit.Security;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Storage;
using MimeKit;
using Newtonsoft.Json;
using System.Data;
using System.Globalization;
using System.Net;
using System.Xml.Linq;


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
        public async Task<ActionResult> CrearPedidoCompletoHibrido([FromBody] PedidoNuevoOptimizado pedidoActual)
        {
            if (pedidoActual == null || pedidoActual.Pedido == null || pedidoActual.Items == null || pedidoActual.Items.Count == 0)
            {
                return BadRequest("Datos inválidos");
            }

            try
            {
                context.Database.SetCommandTimeout(120);
                await context.Database.ExecuteSqlRawAsync("SET LOCK_TIMEOUT 65000");

                var requestId = Guid.NewGuid().ToString();

                await using var connection = context.Database.GetDbConnection();
                await connection.OpenAsync();
                await using var transaction = await context.Database.BeginTransactionAsync();

                PedidoBaseInfo pedidoBase = null;
                int itemsProcesados = 0;

                using (var command = connection.CreateCommand())
                {
                    command.Transaction = transaction.GetDbTransaction();
                    command.CommandText = "dbo.MARKET_CrearPedidoCompletoCabecera_Optimizado";
                    command.CommandType = CommandType.StoredProcedure;
                    command.CommandTimeout = 120;

                    command.Parameters.Add(new SqlParameter("@IdPersona", pedidoActual.Pedido.IdPersona));
                    command.Parameters.Add(new SqlParameter("@IdDireccionEntrega", pedidoActual.Pedido.IdDireccionEntrega));
                    command.Parameters.Add(new SqlParameter("@TotalVenta", pedidoActual.Pedido.TotalVenta));
                    command.Parameters.Add(new SqlParameter("@Peso", pedidoActual.Pedido.Peso));
                    command.Parameters.Add(new SqlParameter("@TipoCp", pedidoActual.Pedido.TipoCp));
                    command.Parameters.Add(new SqlParameter("@RequestId", requestId));

                    using var reader = await command.ExecuteReaderAsync();
                    if (await reader.ReadAsync())
                    {
                        pedidoBase = new PedidoBaseInfo
                        {
                            IdCp = reader.GetInt32(reader.GetOrdinal("IdCp")),
                            IdCpInventario = reader.GetInt32(reader.GetOrdinal("IdCpInventario")),
                            NumCp = reader.IsDBNull(reader.GetOrdinal("NumCp")) ? null : reader.GetString(reader.GetOrdinal("NumCp")),
                            Fecha = reader.GetDateTime(reader.GetOrdinal("Fecha")),
                            RequestId = reader.GetString(reader.GetOrdinal("RequestId"))
                        };
                    }
                }

                if (pedidoBase == null || string.IsNullOrWhiteSpace(pedidoBase.NumCp))
                {
                    return BadRequest("No se pudo crear el pedido base");
                }

                foreach (var item in pedidoActual.Items)
                {
                    using (var itemCommand = connection.CreateCommand())
                    {
                        itemCommand.Transaction = transaction.GetDbTransaction();
                        itemCommand.CommandText = "dbo.MARKET_InsertarItemPedido_Optimizado";
                        itemCommand.CommandType = CommandType.StoredProcedure;
                        itemCommand.CommandTimeout = 60;

                        using (var timeoutCommand = connection.CreateCommand())
                        {
                            timeoutCommand.Transaction = transaction.GetDbTransaction();
                            timeoutCommand.CommandText = "SET LOCK_TIMEOUT 65000";
                            await timeoutCommand.ExecuteNonQueryAsync();
                        }

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
                        itemCommand.Parameters.Add(new SqlParameter("@TieneDescuento", item.TieneDescuento ? 1 : 0));
                        itemCommand.Parameters.Add(new SqlParameter("@IdDescuento", (object?)item.IdDescuento ?? DBNull.Value));

                        await itemCommand.ExecuteNonQueryAsync();
                        itemsProcesados++;
                    }
                }

                await transaction.CommitAsync();

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

                return Ok(respuesta);
            }
            catch (Exception ex)
            {
                return BadRequest($"Error al crear pedido: {ex.Message}");
            }
            finally
            {
                context.Database.SetCommandTimeout(null);
            }
        }
    }
}


//-----------------------
//-----------------------


namespace Csjnet.Entidades
{
    public class ProductoItem
    {
        public int IdCp { get; set; }
        public int IdCpInventario { get; set; }
        public int IdProducto { get; set; }
        public int IdUnidad { get; set; }
        public decimal Peso { get; set; }
        public string Descripcion { get; set; }
        public int Cantidad { get; set; }
        public decimal Precio { get; set; }
        public decimal Total { get; set; }
        public bool TieneBono { get; set; }
        public bool TieneDescuento { get; set; }
        public int? IdDescuento { get; set; }
}
}

//-----------------------
//-----------------------

using System.ComponentModel.DataAnnotations.Schema;
namespace Csjnet.Entidades
{
    public class PedidoNuevoOptimizado
    {
        [NotMapped]
        public PedidoActual Pedido { get; set; }
        [NotMapped]
        public List<ProductoItem> Items { get; set; }
        public string RequestId { get; set; }
    }
}
