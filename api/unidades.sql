USE [SanJuanv97]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

IF OBJECT_ID('dbo.MARKET_ListarUnidadesProducto','P') IS NOT NULL
    DROP PROCEDURE dbo.MARKET_ListarUnidadesProducto;
GO
CREATE PROCEDURE dbo.MARKET_ListarUnidadesProducto
    @IdProducto INT,
    @PrecioBase DECIMAL(18,7) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    SELECT 
        u.PKID       AS IdUnidad,
        u.Factor     AS Factor,
        CASE WHEN @PrecioBase IS NULL THEN NULL ELSE ROUND(@PrecioBase * u.Factor, 2) END AS PrecioUnidad
    FROM Unidad u
    ORDER BY u.Factor ASC;
END
GO
