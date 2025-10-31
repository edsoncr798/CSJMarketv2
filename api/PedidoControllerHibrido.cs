using System;
using System.Collections.Generic;
using System.Data;
using System.Data.SqlClient;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;

namespace CSJ_Market.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class PedidoControllerHibrido : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly string _connectionString;

        public PedidoControllerHibrido(IConfiguration configuration)
        {
            _configuration = configuration;
            _connectionString = _configuration.GetConnectionString("DefaultConnection");
        }

        [HttpPost("CrearPedidoCompletoHibrido")]
        public async Task<IActionResult> CrearPedidoCompletoHibrido([FromBody] PedidoHibridoRequest request)
        {
            using (var connection = new SqlConnection(_connectionString))
            {
                await connection.OpenAsync();
                
                using (var transaction = connection.BeginTransaction())
                {
                    try
                    {
                        // 1. Generar RequestId único
                        var requestId = Guid.NewGuid().ToString();

                        // 2. Crear pedido base (solo cabecera) sin items
                        var pedidoBase = await CrearPedidoBase(connection, transaction, request, requestId);
                        
                        if (pedidoBase == null)
                        {
                            transaction.Rollback();
                            return BadRequest(new { error = "Error al crear el pedido base" });
                        }

                        // 3. Procesar items uno por uno con transacciones individuales
                        var itemsProcesados = 0;
                        foreach (var item in request.Productos)
                        {
                            var itemResult = await InsertarItemPedido(connection, transaction, 
                                pedidoBase.IdCp, pedidoBase.IdCpInventario, item, requestId);
                            
                            if (!itemResult)
                            {
                                transaction.Rollback();
                                return BadRequest(new { error = $"Error al procesar el item: {item.Descripcion}" });
                            }
                            itemsProcesados++;
                        }

                        // 4. Confirmar transacción principal
                        transaction.Commit();

                        // 5. Retornar respuesta exitosa
                        return Ok(new
                        {
                            IdCp = pedidoBase.IdCp,
                            IdCpInventario = pedidoBase.IdCpInventario,
                            NumCp = pedidoBase.NumCp,
                            Fecha = pedidoBase.Fecha,
                            RequestId = requestId,
                            ItemsProcesados = itemsProcesados,
                            Mensaje = "Pedido creado exitosamente"
                        });
                    }
                    catch (Exception ex)
                    {
                        transaction.Rollback();
                        return StatusCode(500, new { error = ex.Message });
                    }
                }
            }
        }

        private async Task<PedidoBaseInfo> CrearPedidoBase(SqlConnection connection, SqlTransaction transaction, 
            PedidoHibridoRequest request, string requestId)
        {
            using (var command = new SqlCommand("dbo.MARKET_CrearPedidoCompletoCabecera_Optimizado", connection))
            {
                command.CommandType = CommandType.StoredProcedure;
                command.Transaction = transaction;
                command.CommandTimeout = 120; // 2 minutos timeout

                // Parámetros del stored procedure
                command.Parameters.AddWithValue("@IdPersona", request.IdPersona);
                command.Parameters.AddWithValue("@IdDireccionEntrega", request.IdDireccionEntrega);
                command.Parameters.AddWithValue("@TotalVenta", request.TotalVenta);
                command.Parameters.AddWithValue("@Peso", request.Peso);
                command.Parameters.AddWithValue("@TipoCp", request.TipoCp);
                command.Parameters.AddWithValue("@RequestId", requestId);

                using (var reader = await command.ExecuteReaderAsync())
                {
                    if (await reader.ReadAsync())
                    {
                        return new PedidoBaseInfo
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
            return null;
        }

        private async Task<bool> InsertarItemPedido(SqlConnection connection, SqlTransaction transaction, 
            int idCp, int idCpInventario, ProductoItem item, string requestId)
        {
            try
            {
                using (var command = new SqlCommand("dbo.MARKET_InsertarItemPedido_Optimizado", connection))
                {
                    command.CommandType = CommandType.StoredProcedure;
                    command.Transaction = transaction;
                    command.CommandTimeout = 60; // 1 minuto timeout por item

                    // Establecer LOCK_TIMEOUT para evitar bloqueos prolongados
                    using (var timeoutCommand = new SqlCommand("SET LOCK_TIMEOUT 65000", connection))
                    {
                        timeoutCommand.Transaction = transaction;
                        await timeoutCommand.ExecuteNonQueryAsync();
                    }

                    // Parámetros del stored procedure
                    command.Parameters.AddWithValue("@IdCp", idCp);
                    command.Parameters.AddWithValue("@IdCpInventario", idCpInventario);
                    command.Parameters.AddWithValue("@IdProducto", item.IdProducto);
                    command.Parameters.AddWithValue("@IdUnidad", item.IdUnidad);
                    command.Parameters.AddWithValue("@Cantidad", item.Cantidad);
                    command.Parameters.AddWithValue("@Peso", item.Peso);
                    command.Parameters.AddWithValue("@Precio", item.Precio);
                    command.Parameters.AddWithValue("@Total", item.Total);
                    command.Parameters.AddWithValue("@Descripcion", item.Descripcion);
                    command.Parameters.AddWithValue("@TieneBono", item.TieneBono ? 1 : 0); // NUEVO: Indica si tiene bonificación
                    command.Parameters.AddWithValue("@RequestId", requestId);

                    await command.ExecuteNonQueryAsync();
                    return true;
                }
            }
            catch (Exception)
            {
                return false;
            }
        }
    }

    // Clases DTO para las peticiones
    public class PedidoHibridoRequest
    {
        public int IdPersona { get; set; }
        public int IdDireccionEntrega { get; set; }
        public decimal TotalVenta { get; set; }
        public decimal Peso { get; set; }
        public int TipoCp { get; set; }
        public List<ProductoItem> Productos { get; set; }
    }

    public class ProductoItem
    {
        public int IdProducto { get; set; }
        public int IdUnidad { get; set; }
        public int Cantidad { get; set; }
        public decimal Peso { get; set; }
        public decimal Precio { get; set; }
        public decimal Total { get; set; }
        public string Descripcion { get; set; }
        public bool TieneBono { get; set; } // NUEVO: Indica si el producto tiene bonificación
    }

    public class PedidoBaseInfo
    {
        public int IdCp { get; set; }
        public int IdCpInventario { get; set; }
        public string NumCp { get; set; }
        public DateTime Fecha { get; set; }
        public string RequestId { get; set; }
    }
}