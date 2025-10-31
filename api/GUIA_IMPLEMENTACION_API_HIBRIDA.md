# Guía de Implementación - API Híbrida CrearPedidoCompletoHibrido

## 📋 Resumen de la Solución

La API híbrida resuelve el problema de congelamiento del ERP al procesar pedidos grandes. En lugar de procesar todos los items en una sola transacción masiva, crea la cabecera primero y luego procesa cada item individualmente.

## 🏗️ Arquitectura de la Solución

### Componentes Principales:

1. **PedidoControllerHibrido.cs** - Controlador .NET con endpoint `CrearPedidoCompletoHibrido`
2. **MARKET_CrearPedidoCompletoCabecera_Optimizado.sql** - SP para crear solo la cabecera
3. **MARKET_InsertarItemPedido_Optimizado.sql** - SP existente para items individuales

### Flujo de Ejecución:

```
┌─────────────────┐    ┌──────────────────────┐    ┌──────────────────┐
│   Android App   │───▶│  API Híbrida .NET   │───▶│  SQL Server      │
│                 │    │                      │    │                  │
│ POST:           │    │ 1. Generar RequestId │    │ 1. SP Cabecera   │
│ CrearPedido     │    │ 2. Crear cabecera    │    │ 2. SP Items x1   │
│ Completo        │    │ 3. Procesar items    │    │                  │
└─────────────────┘    └──────────────────────┘    └──────────────────┘
```

## 🔧 Implementación Paso a Paso

### Paso 1: Ejecutar el Stored Procedure de Cabecera

```sql
-- Ejecutar el archivo: MARKET_CrearPedidoCompletoCabecera_Optimizado.sql
-- Este SP crea solo la cabecera sin procesar items XML
```

### Paso 2: Implementar el Controlador .NET

El controlador `PedidoControllerHibrido.cs` ya está creado con:

- ✅ Endpoint `POST api/PedidoControllerHibrido/CrearPedidoCompletoHibrido`
- ✅ Uso del nuevo SP de cabecera
- ✅ Procesamiento individual de items
- ✅ Timeouts apropiados (120s total, 65s lock timeout)
- ✅ Manejo de errores con rollback
- ✅ Formato JSON compatible

### Paso 3: Configurar la Conexión

Actualizar `appsettings.json` con los datos de conexión reales:

```json
{
  "ConnectionStrings": {
    "DefaultConnection": "Server=tu_servidor;Database=SanJuanv97;User Id=tu_usuario;Password=tu_password;TrustServerCertificate=true;"
  }
}
```

### Paso 4: Registrar el Controlador en Startup.cs

```csharp
public void ConfigureServices(IServiceCollection services)
{
    services.AddControllers();
    // ... otros servicios
}

public void Configure(IApplicationBuilder app, IWebHostEnvironment env)
{
    // ... middleware
    app.UseEndpoints(endpoints =>
    {
        endpoints.MapControllers();
    });
}
```

## 📊 Ventajas de la Solución Híbrida

| Característica | API Original | API Híbrida | Beneficio |
|----------------|--------------|-------------|-----------|
| **Transacciones** | 1 masiva | Múltiples pequeñas | Reduce bloqueos |
| **Tiempo de bloqueo** | Minutos | Segundos | ERP no congela |
| **Procesamiento** | Todo o nada | Por item | Más control |
| **Rollback** | Total | Parcial | Mejor manejo de errores |
| **Escalabilidad** | Limitada | Alta | Mejor performance |

## 🧪 Testing y Validación

### Prueba de Rendimiento

```bash
# Ejecutar script de monitoreo
python monitor_pedidos.py
```

### Validaciones a Realizar:

1. ✅ **Tiempo de respuesta**: < 30 segundos
2. ✅ **Sin congelamiento ERP**: Sistema operativo durante pedidos
3. ✅ **Idempotencia**: Mismo RequestId = mismo resultado
4. ✅ **Formato JSON**: Compatible con Android app
5. ✅ **Manejo de errores**: Rollback apropiado

## 📱 Integración con Android

### URL del Endpoint:
```
POST https://tu-servidor/api/PedidoControllerHibrido/CrearPedidoCompletoHibrido
```

### Formato de Request (JSON):
```json
{
  "IdPersona": 12345,
  "IdDireccionEntrega": 67890,
  "TotalVenta": 150.50,
  "Peso": 2.5,
  "TipoCp": 1001,
  "Productos": [
    {
      "IdProducto": 101,
      "IdUnidad": 1,
      "Cantidad": 2,
      "Peso": 1.0,
      "Precio": 50.25,
      "Total": 100.50,
      "Descripcion": "Producto A"
    },
    {
      "IdProducto": 102,
      "IdUnidad": 1,
      "Cantidad": 1,
      "Peso": 1.5,
      "Precio": 50.00,
      "Total": 50.00,
      "Descripcion": "Producto B"
    }
  ]
}
```

### Formato de Response (JSON):
```json
{
  "IdCp": 123456,
  "IdCpInventario": 789012,
  "NumCp": "P001-00012345",
  "Fecha": "2024-01-15T10:30:00",
  "RequestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "ItemsProcesados": 2,
  "Mensaje": "Pedido creado exitosamente"
}
```

## ⚠️ Consideraciones Importantes

### Seguridad:
- Validar autenticación antes de procesar
- Implementar límites de tasa (rate limiting)
- Sanitizar inputs para prevenir SQL injection

### Monitoreo:
- Registrar tiempos de ejecución
- Monitorear errores y reintentos
- Alertar si el tiempo excede 30 segundos

### Mantenimiento:
- Actualizar índices regularmente
- Revisar fragmentación de índices
- Monitorear uso de memoria y CPU

## 🚀 Deployment

1. **Backup**: Respaldar base de datos antes de implementar
2. **Staging**: Probar en ambiente de prueba primero
3. **Rollback**: Tener plan de reversión listo
4. **Monitorización**: Observar métricas durante 24-48 horas

## 📞 Soporte

Si encuentras problemas:

1. Verificar logs del servidor
2. Revisar tiempos de ejecución con `monitor_pedidos.py`
3. Validar índices con queries de diagnóstico
4. Contactar al equipo de desarrollo con:
   - Mensaje de error exacto
   - Tiempo de ocurrencia
   - Datos del pedido que falló

---

**✅ Resultado Esperado**: ERP sin congelamiento, pedidos procesados en < 30 segundos, sistema estable y escalable.