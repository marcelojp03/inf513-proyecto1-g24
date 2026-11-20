BEGIN;
SET LOCAL search_path TO banco_talentos_mail, pg_catalog;
DO $$
DECLARE
    v_correo text := 'mjimenezp126@ficct.uagrm.edu.bo';
    v_user bigint;
    v_rol text;
BEGIN
    INSERT INTO usuario(correo, rol) VALUES (v_correo, 'RECLUTADOR')
    ON CONFLICT (correo) DO NOTHING;

    SELECT id, rol INTO v_user, v_rol FROM usuario WHERE correo = v_correo;
    IF v_rol <> 'RECLUTADOR' THEN
        RAISE EXCEPTION 'Ese correo ya existe con rol %.', v_rol;
    END IF;

    RAISE NOTICE 'Reclutador listo: usuario %', v_user;
END;
$$;
COMMIT;

SET search_path TO banco_talentos_mail, pg_catalog;
SELECT id, correo, rol, activo FROM usuario ORDER BY id;
