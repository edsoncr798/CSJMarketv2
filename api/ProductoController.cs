using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Data.SqlClient;
using System.Data;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json;

namespace Csjnet.Controllers
{
    [ApiController]
    [Route("api")]
    public class ProductoController : ControllerBase
    {
        private readonly ApplicationDbContext _context;

        public ProductoController(ApplicationDbContext context)
        {
            _context = context;
        }

        [HttpGet("productos")]
        public async Task<IActionResult> GetProductos([FromQuery] int pagina = 1, [FromQuery] int limite = 20)
        {
            try
            {
                _context.Database.SetCommandTimeout(60);

                using var conn = (SqlConnection)_context.Database.GetDbConnection();
                if (conn.State != ConnectionState.Open)
                    await conn.OpenAsync();

                using var cmd = new SqlCommand("dbo.MARKET_listarProducto", conn)
                {
                    CommandType = CommandType.StoredProcedure
                };
                cmd.Parameters.AddWithValue("@pagina", pagina);
                cmd.Parameters.AddWithValue("@limite", limite);

                var arr = new JArray();
                using var reader = await cmd.ExecuteReaderAsync();
                while (await reader.ReadAsync())
                {
                    var obj = new JObject();
                    for (int i = 0; i < reader.FieldCount; i++)
                    {
                        string name = reader.GetName(i);
                        var val = reader.IsDBNull(i) ? null : reader.GetValue(i);
                        obj[name] = val == null ? JValue.CreateNull() : JToken.FromObject(val);
                    }
                    arr.Add(obj);
                }

                return Content(arr.ToString(Formatting.None), "application/json");
            }
            catch (System.Exception ex)
            {
                return BadRequest($"Error al listar productos: {ex.Message}");
            }
            finally
            {
                _context.Database.SetCommandTimeout(null);
            }
        }

        [HttpGet("productoscategoria")]
        public async Task<IActionResult> GetProductosPorCategoria([FromQuery] int idProveedor, [FromQuery] int pagina = 1, [FromQuery] int limite = 20)
        {
            try
            {
                _context.Database.SetCommandTimeout(60);

                using var conn = (SqlConnection)_context.Database.GetDbConnection();
                if (conn.State != ConnectionState.Open)
                    await conn.OpenAsync();

                using var cmd = new SqlCommand("dbo.MARKET_listarPorductoCategoria", conn)
                {
                    CommandType = CommandType.StoredProcedure
                };
                cmd.Parameters.AddWithValue("@IdProveedor", idProveedor);
                cmd.Parameters.AddWithValue("@pagina", pagina);
                cmd.Parameters.AddWithValue("@limite", limite);

                var arr = new JArray();
                using var reader = await cmd.ExecuteReaderAsync();
                while (await reader.ReadAsync())
                {
                    var obj = new JObject();
                    for (int i = 0; i < reader.FieldCount; i++)
                    {
                        string name = reader.GetName(i);
                        var val = reader.IsDBNull(i) ? null : reader.GetValue(i);
                        obj[name] = val == null ? JValue.CreateNull() : JToken.FromObject(val);
                    }
                    arr.Add(obj);
                }

                return Content(arr.ToString(Formatting.None), "application/json");
            }
            catch (System.Exception ex)
            {
                return BadRequest($"Error al listar productos por categoría: {ex.Message}");
            }
            finally
            {
                _context.Database.SetCommandTimeout(null);
            }
        }
    }
}
