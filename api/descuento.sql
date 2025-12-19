USE [SanJuanv97]
GO
/****** Object:  StoredProcedure [dbo].[MARKET_InsertarItemPedido_Optimizado]    Script Date: 27/11/2025 15:46:36 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

/*
MARKET_InsertarItemPedido_Optimizado
- Inserta un ítem en ItemCp, ItemPedido, ItemProducto
- Integra idempotencia por RequestId y bloqueo lógico (sp_getapplock)
- Valida duplicados por ítem para evitar re-inserciones en reintentos
- Gestiona bonificaciones vía MARKET_GestionarBonificacionPedidov2 cuando aplica
- Minimiza bloqueos: transacción corta, LOCK_TIMEOUT, ROWLOCK en UPDATE
- Genera PK de ItemCp por SEQUENCE si existe, o pFB_GenerarID como fallback
*/
ALTER PROCEDURE [dbo].[MARKET_InsertarItemPedido_Optimizado]
    @IdCp INT,
    @IdCpInventario INT,
    @IdProducto INT,
    @IdUnidad INT,
    @Peso DECIMAL(18, 7),
    @Descripcion VARCHAR(255),
    @Cantidad INT,
    @Precio DECIMAL(18, 7),
    @Total DECIMAL(18, 7),
    @RequestId VARCHAR(50),
    @IdAlmacen INT = 1,
    @TieneBono BIT = 0,
	@TieneDescuento BIT = 0,
	@IdDescuento int = null
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    SET LOCK_TIMEOUT 65000;
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

    DECLARE @IdItemCp BIGINT;
    DECLARE @TipoItemCp INT = 1;
    DECLARE @FactorUnidad INT = 1;
    DECLARE @CantidadFactor INT;
    DECLARE @HasBonus BIT = 0;
    DECLARE @lockKey NVARCHAR(200);
    DECLARE @lockResult INT;

    BEGIN TRY
        -- Validar RequestId vinculado al pedido
        IF NOT EXISTS (
            SELECT 1 FROM dbo.PedidoRequestId WITH (NOLOCK)
            WHERE RequestId = @RequestId AND IdCp = @IdCp
        )
        BEGIN
            RAISERROR('Transacción inválida: RequestId no asociado al pedido.', 16, 1);
            RETURN;
        END

        BEGIN TRANSACTION;

        -- Bloqueo lógico por recurso para evitar duplicados concurrentes del mismo ítem
        SET @lockKey = N'Pedido:' + CAST(@IdCp AS NVARCHAR(20)) +
                        N':Req:' + @RequestId +
                        N':Prod:' + CAST(@IdProducto AS NVARCHAR(20)) +
                        N':Unid:' + CAST(@IdUnidad AS NVARCHAR(20));
        EXEC @lockResult = sp_getapplock @Resource = @lockKey, @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 5000;
        IF (@lockResult < 0)
        BEGIN
            ROLLBACK TRANSACTION;
            RAISERROR('No se pudo obtener bloqueo de concurrencia para insertar el ítem base.', 16, 1);
            RETURN;
        END

        -- Factor de unidad y cantidad factor (lectura con NOLOCK para evitar bloqueos)
        SELECT @FactorUnidad = ISNULL(u.Factor, 1)
        FROM dbo.Unidad u WITH (NOLOCK)
        WHERE u.PKID = @IdUnidad;

        SET @CantidadFactor = (@Cantidad * @FactorUnidad);

        -- ¿Producto con bonificación vigente?
        IF @TieneBono = 1
            SET @HasBonus = 1;

        -- Validación de duplicado del ítem base (incluye precio y cantidad base)
        IF EXISTS (
            SELECT 1
            FROM dbo.ItemProducto ip WITH (NOLOCK)
            INNER JOIN dbo.ItemCp ic WITH (NOLOCK) ON ic.PKID = ip.PKID
            WHERE ic.IDCp = @IdCp
              AND ip.IDProducto = @IdProducto
              AND ip.IDUnidad = @IdUnidad
              AND ISNULL(ic.Bonificacion, 0) = 0
              AND ic.TipoItemCp = 1
              AND ISNULL(ic.Anulado, 0) = 0
              AND ip.CantidadBase = @CantidadFactor
              AND ic.ValorUnitario = @Precio
        )
        BEGIN
            ROLLBACK TRANSACTION;
            RETURN; -- evitar inserción duplicada
        END

        	IF (ISNULL(@TieneDescuento, 0) = 1)
        	BEGIN
        		EXEC MARKET_GestionarDescuentoPedido
        			@IdCp = @IdCp,
        			@IdCpInventario = @IdCpInventario,
        			@IdUnidad = @IdUnidad,
        			@IdProducto = @IdProducto,
        			@Descripcion = @Descripcion,
        			@Peso = @Peso,
        			@Cantidad = @Cantidad,
        			@precioUnitario = @Precio,
        			@Total = @Total,
        			@IdDescuento = @IdDescuento

        		-- Si el SP de descuento no insertó ninguna línea (no aplicó regla), insertar ítem base
        		IF NOT EXISTS (
        			SELECT 1
        			FROM dbo.ItemProducto ip WITH (NOLOCK)
        			INNER JOIN dbo.ItemCp ic WITH (NOLOCK) ON ic.PKID = ip.PKID
        			WHERE ic.IDCp = @IdCp
        			  AND ip.IDProducto = @IdProducto
        			  AND ip.IDUnidad = @IdUnidad
        			  AND ISNULL(ic.Bonificacion, 0) = 0
        			  AND ic.TipoItemCp = 1
        			  AND ISNULL(ic.Anulado, 0) = 0
        		)
        		BEGIN
        			-- Generar ID para ItemCp
        			DECLARE @IdItemCpInt2 INT;
        			EXEC pFB_GenerarID 'ItemCp', @IdItemCpInt2 OUTPUT;
        			SET @IdItemCp = @IdItemCpInt2;

        			INSERT INTO dbo.ItemCp
        			VALUES (
        				@IdItemCp, @IdCp, @Cantidad, @Descripcion, @TipoItemCp,
        				@Total, @Precio, 0, 0, 0, 0, @Total, @Total, 0, 0, 0, 0, 0, 0,
        				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        				@Total, 1, 0, 0
        			);

        			INSERT INTO dbo.ItemPedido
        			VALUES (
        				@IdItemCp, 0, 0, @Precio, 0, 0, 0, 0, @Cantidad, 0, 0, 0, 0, 0
        			);

        			INSERT INTO dbo.ItemProducto
        			VALUES (
        				@IdItemCp, @IdProducto, @IdUnidad, @FactorUnidad, @CantidadFactor,
        				1, 0, 0, '', '', 0, @Peso, 0, 0, 0, 0, 0, 0, 0,
        				0, 0, 0, 0, 0, @IdCpInventario, 1
        			);

        			UPDATE pa WITH (ROWLOCK)
        			SET pa.StockPorEntregar = pa.StockPorEntregar + @CantidadFactor
        			FROM dbo.ProductoAlmacen pa
        			WHERE pa.IDProducto = @IdProducto AND pa.IDAlmacen = @IdAlmacen;
        		END
        	END
        	ELSE
        	BEGIN

			-- Generar ID para ItemCp (correlativo global del sistema)
			DECLARE @IdItemCpInt INT;
			EXEC pFB_GenerarID 'ItemCp', @IdItemCpInt OUTPUT;
			SET @IdItemCp = @IdItemCpInt;

			-- Inserción ítem base
			INSERT INTO dbo.ItemCp
			VALUES (
				@IdItemCp, @IdCp, @Cantidad, @Descripcion, @TipoItemCp,
				@Total, @Precio, 0, 0, 0, 0, @Total, @Total, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				@Total, 1, 0, 0
			);

			INSERT INTO dbo.ItemPedido
			VALUES (
				@IdItemCp, 0, 0, @Precio, 0, 0, 0, 0, @Cantidad, 0, 0, 0, 0, 0
			);

			INSERT INTO dbo.ItemProducto
			VALUES (
				@IdItemCp, @IdProducto, @IdUnidad, @FactorUnidad, @CantidadFactor,
				1, 0, 0, '', '', 0, @Peso, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, @IdCpInventario, 1
			);

			-- Actualizar StockPorEntregar minimizando bloqueos
			UPDATE pa WITH (ROWLOCK)
			SET pa.StockPorEntregar = pa.StockPorEntregar + @CantidadFactor
			FROM dbo.ProductoAlmacen pa
			WHERE pa.IDProducto = @IdProducto AND pa.IDAlmacen = @IdAlmacen;

			-- Gestionar bonificación si aplica
			IF (ISNULL(@HasBonus, 0) = 1)
			BEGIN
				EXEC dbo.MARKET_GestionarBonificacionPedidov2
					@IdCp = @IdCp,
					@IdCpInventario = @IdCpInventario,
					@IdProducto = @IdProducto,
					@IdUnidad = @IdUnidad,
					@Descripcion = @Descripcion,
					@Cantidad = @Cantidad,
					@Total = @Total,
					@HasBonus = @HasBonus;
			END
		END
        	UPDATE Cp WITH (ROWLOCK, UPDLOCK)
        	SET Total = t.SumTotal,
        		ValorVenta = t.SumTotal,
        		ValorExonerado = t.SumTotal,
        		SubTotal = t.SumSubTotal
        	FROM (
        		SELECT 
        			ROUND(ISNULL(SUM(ISNULL(ValorVenta,0)),0), 2)     AS SumTotal,
        			ROUND(ISNULL(SUM(ISNULL(SubTotal,0)),0), 2)       AS SumSubTotal
        		FROM ItemCp WITH (UPDLOCK, HOLDLOCK)
        		WHERE IDCp = @IdCp
        		  AND ISNULL(Anulado,0) = 0
        	) AS t
        	WHERE PKID = @IdCp;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        DECLARE @ERROR_MSG NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ERROR_LINE INT = ERROR_LINE();
        RAISERROR('Error en ítem en línea %d: %s', 16, 1, @ERROR_LINE, @ERROR_MSG);
        RETURN;
    END CATCH
END

--------------
--------------

USE [SanJuanv97]
GO
/****** Object:  StoredProcedure [dbo].[MARKET_GestionarDescuentoPedido]    Script Date: 21/11/2025 10:59:49 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

USE [SanJuanv97]
GO
/****** Object:  StoredProcedure [dbo].[MARKET_GestionarDescuentoPedido]    Script Date: 21/11/2025 10:59:49 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROCEDURE [dbo].[MARKET_GestionarDescuentoPedido]
	@IdCp int,
	@IdCpInventario INT,
	@IdUnidad int,
	@IdProducto int,
	@Descripcion varchar(255),
	@Peso decimal(18,7),
	@Cantidad int,
	@PrecioUnitario Decimal(18,7),
	@Total decimal(18,7),
	@IdDescuento int = null
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    BEGIN TRANSACTION;

    BEGIN TRY
        DECLARE 
            @TipoItemCp INT = 1,
            @HasBonusApplied BIT = 0,
			@PorcentajeDescto decimal(18,7),
			@DescuentoAplicado DECIMAL(18,7) = 0,
			@CantidadFactor INT,
			@FactorUnidad INT = 1,
            @TipoCondicion NVARCHAR(50), @Condicion NVARCHAR(50), @ValorDesde DECIMAL(18,2), @ValorHasta DECIMAL(18,2),
            @lockResult INT, @TotalConDescuento decimal(18,7),
            @Codigo NVARCHAR(50), @IdItemCpPO INT;

        -- Condición de bonificación para el producto base
        SELECT top 1
            @TipoCondicion = TipoCondicion,
            @Condicion = Condicion,
            @ValorDesde = ValorDesde,
            @ValorHasta = ValorHasta,
            @Codigo = CodigoProducto,
			@PorcentajeDescto = PorcentajeDescuento
        FROM dbo.V_CondicionesDescuentoSimplificada
        WHERE IdProducto = @IdProducto
			AND IDDescuento = @IdDescuento 


        SELECT @FactorUnidad = ISNULL(u.Factor, 1)
        FROM dbo.Unidad u WITH (NOLOCK)
        WHERE u.PKID = @IdUnidad;

        SET @CantidadFactor = (@Cantidad * @FactorUnidad);

        -- Si no llegó IdDescuento o la regla no fue encontrada, salir limpio
        IF (@IdDescuento IS NULL OR @PorcentajeDescto IS NULL)
        BEGIN
            SET @DescuentoAplicado = 0;
            COMMIT TRANSACTION;
            RETURN;
        END

        -- Evaluación de reglas
        -- Normalizar límite superior: NULL equivale a tramo abierto (infinito)
        SET @ValorHasta = ISNULL(@ValorHasta, 999999999.99);

        IF @TipoCondicion = 'CantidadBase'
        BEGIN
            IF (@Condicion = 'Entre' AND @CantidadFactor >= @ValorDesde AND @CantidadFactor <= @ValorHasta)
                SET @HasBonusApplied = 1;
            ELSE IF (@Condicion = '>=' AND @CantidadFactor >= @ValorDesde)
                SET @HasBonusApplied = 1;
        END
        ELSE IF @TipoCondicion = 'ValorVenta'
        BEGIN
            IF (@Condicion = 'Entre' AND @Total >= @ValorDesde AND @Total <= @ValorHasta)
                SET @HasBonusApplied = 1;
            ELSE IF (@Condicion = '>=' AND @Total >= @ValorDesde)
                SET @HasBonusApplied = 1;
        END

        IF @HasBonusApplied = 0
        BEGIN
			SET @DescuentoAplicado = 0;
            RAISERROR('No se cumple condición de descuento para %s.', 10, 1, @Codigo);
            COMMIT TRANSACTION;
            RETURN;
        END
		ELSE
		BEGIN
			SET @DescuentoAplicado = ROUND(@Total * (@PorcentajeDescto / 100.0), 2);

			SET @TotalConDescuento = @Total - @DescuentoAplicado

			-- Insertar nuevo ítem de bonificación
            EXEC pFB_GenerarID 'ItemCp', @IdItemCpPO OUTPUT;

				-- Inserción ítem base
			INSERT INTO dbo.ItemCp
			VALUES (
				@IdItemCpPO, @IdCp, @Cantidad, @Descripcion, @TipoItemCp,
				@TotalConDescuento, @PrecioUnitario, 0, 0, 0, 0, @TotalConDescuento, @Total, 0, 0, 0, @DescuentoAplicado, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				@TotalConDescuento, 1, 0, 0
			);

			INSERT INTO dbo.ItemPedido
			VALUES (
				@IdItemCpPO, 0, 0, @PrecioUnitario, 0, 0, 0, 0, @Cantidad, 0, 0, 0, 0, 0
			);

			INSERT INTO dbo.ItemProducto
			VALUES (
				@IdItemCpPO, @IdProducto, @IdUnidad, @FactorUnidad, @CantidadFactor,
				1, 0, 0, '', '', 0, @Peso, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, @IdCpInventario, 1
			);

			-- Actualizar StockPorEntregar minimizando bloqueos
			UPDATE pa 
			SET pa.StockPorEntregar = pa.StockPorEntregar + @CantidadFactor
			FROM dbo.ProductoAlmacen pa
			WHERE pa.IDProducto = @IdProducto AND pa.IDAlmacen = 1;


		END

        COMMIT TRANSACTION;
        RETURN;
        
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        RAISERROR('Error en Descuento: %s', 16, 1);
        RETURN;
    END CATCH
END
