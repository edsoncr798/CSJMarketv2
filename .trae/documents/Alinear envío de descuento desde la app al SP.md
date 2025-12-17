## Diagnóstico
- El SP de descuento funciona cuando lo ejecutas manualmente porque le pasas `@IdDescuento` y los demás parámetros necesarios.
- Desde la app, el endpoint `/api/pedido/CrearPedidoCompletoHibrido` no está enviando el descuento al SP:
  - El método `InsertarItemPedido` solo pasa `@TieneBono` y no pasa `@TieneDescuento` ni `@IdDescuento` (`api/PedidoControllerHibrido.cs:139-151`).
  - El DTO usado por el API no incluye los campos de descuento (`api/PedidoControllerHibrido.cs:174-184`).
- En la app sí se generan y envían `TieneDescuento` y `IdDefinicionDescuento` en cada item (`DireccionEntrega.java:459-461` y `ProductoItem.java:15-21`), pero ASP.NET Core ignora propiedades desconocidas del DTO, por eso el descuento no llega al SP.

## Cambios propuestos (servidor)
1. Extender el DTO `ProductoItem` del API para incluir:
   - `public bool TieneDescuento { get; set; }`
   - `public int? IdDefinicionDescuento { get; set; }` (si tu SP espera `@IdDescuento`, usar ese nombre en el parámetro SQL).
2. Pasar estos parámetros al SP en `InsertarItemPedido`:
   - `command.Parameters.AddWithValue("@TieneDescuento", item.TieneDescuento ? 1 : 0);`
   - `command.Parameters.AddWithValue("@IdDescuento", (object)item.IdDefinicionDescuento ?? DBNull.Value);`
3. Confirmar que `dbo.MARKET_InsertarItemPedido_Optimizado` acepta `@TieneDescuento BIT` y `@IdDescuento INT NULL`. Si no, agregar esos parámetros en el SP y encaminar al `MARKET_GestionarDescuentoPedido` con `@IdDescuento`.
4. Asegurar que, si `@IdDescuento` llega null pero `@TieneDescuento = 1`, el SP seleccione la regla correcta de forma determinista (por rango `ValorDesde/ValorHasta` y tipo de condición) o haga early return si no aplica.

## Cambios opcionales (cliente)
- Log de verificación previo al POST para imprimir el JSON completo del pedido, confirmando que cada `item` incluye `TieneDescuento` y `IdDefinicionDescuento`.

## Verificación
1. Caso Popeye 3.5%: agregar 1 unidad y observar que el `RequestId` devuelve `IdCp` y el descuento aplicado.
2. Probar producto con 2 reglas: validar que el cliente envía el `IdDefinicionDescuento` seleccionado y el SP aplica la regla exacta.
3. Validar caso sin descuento: `TieneDescuento=false` y `IdDefinicionDescuento=null` no deben afectar totales.
4. Revisar que `Cp.Total` no reciba `NULL` al recalcular.

## Entregables
- Actualización de `PedidoControllerHibrido.cs` con DTO extendido y parámetros extra en el SP call.
- (Si hace falta) actualización de `MARKET_InsertarItemPedido_Optimizado` para aceptar y usar esos parámetros.

¿Confirmas que aplique estos cambios en la rama `features/logica-descuentos`?