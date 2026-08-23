-- Fix: factura_id debe ser nullable en pagos y recibos
-- (Hibernate ddl-auto: update no elimina restricciones NOT NULL existentes)

DO $$
BEGIN
  -- pagos.factura_id: drop NOT NULL si existe
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'pagos' AND column_name = 'factura_id'
    AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE pagos ALTER COLUMN factura_id DROP NOT NULL;
  END IF;

  -- recibos.factura_id: drop NOT NULL si existe
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'recibos' AND column_name = 'factura_id'
    AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE recibos ALTER COLUMN factura_id DROP NOT NULL;
  END IF;
END $$;
