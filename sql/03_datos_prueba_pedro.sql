BEGIN;
SET LOCAL search_path TO banco_talentos_mail, pg_catalog;
DO $$
DECLARE
    v_correo text := 'elproo16399@gmail.com';
    v_cargo bigint;
    v_tec bigint;
    v_per bigint;
    v_admin bigint;
    v_recl bigint;
    v_user bigint;
    v_exp bigint;
    v_plan bigint;
BEGIN
    SELECT id INTO v_cargo FROM cargo WHERE nombre = 'Analista de datos' ORDER BY id LIMIT 1;
    SELECT id INTO v_tec FROM instrumento WHERE codigo = 'TEC-01' ORDER BY id LIMIT 1;
    SELECT id INTO v_per FROM instrumento WHERE codigo = 'PER-01' ORDER BY id LIMIT 1;
    SELECT id INTO v_admin FROM usuario WHERE rol = 'ADMINISTRADOR' ORDER BY id LIMIT 1;
    SELECT id INTO v_recl FROM usuario WHERE correo = 'mendietaletizia@gmail.com';
    IF v_cargo IS NULL OR v_tec IS NULL OR v_per IS NULL OR v_admin IS NULL OR v_recl IS NULL THEN
        RAISE EXCEPTION 'Primero cargá los datos de demostración (02_datos_demo.sql).';
    END IF;

    INSERT INTO usuario(correo, rol) VALUES (v_correo, 'CANDIDATO')
    ON CONFLICT (correo) DO UPDATE SET activo = true;
    SELECT id INTO v_user FROM usuario WHERE correo = v_correo;

    IF EXISTS (SELECT 1 FROM expediente WHERE usuario_id = v_user AND cargo_id = v_cargo) THEN
        RAISE NOTICE 'Pedro ya tiene el expediente de prueba.';
        RETURN;
    END IF;

    INSERT INTO expediente(usuario_id, tipo, cargo_id, nombre, carrera, experiencia_meses, competencias, disponible, estado)
    VALUES (v_user, 'B', v_cargo, 'Pedro Peña', 'Administracion', 12,
            string_to_array('excel;atencion', ';'), true, 'APTO')
    RETURNING id INTO v_exp;

    INSERT INTO consentimiento(expediente_id, finalidad, otorgado, destino)
    VALUES (v_exp, 'EVALUACION', true, NULL)
    ON CONFLICT (expediente_id, finalidad) DO UPDATE SET otorgado = true, destino = NULL, actualizado_en = now();

    INSERT INTO plan(expediente_id) VALUES (v_exp) RETURNING id INTO v_plan;
    INSERT INTO evaluacion(plan_id, instrumento_id) VALUES (v_plan, v_tec), (v_plan, v_per);

    INSERT INTO evento_expediente(expediente_id, actor_id, operacion, detalle) VALUES
        (v_exp, v_admin, 'VINCEXP', 'Participacion de prueba creada'),
        (v_exp, v_recl, 'CLASEXP', 'APTO | Perfil verificado'),
        (v_exp, v_recl, 'ASIGPLAN', 'Plan asignado con 2 instrumentos');

    RAISE NOTICE 'Pedro listo: usuario %, expediente %, plan %', v_user, v_exp, v_plan;
END;
$$;
COMMIT;

SET search_path TO banco_talentos_mail, pg_catalog;
SELECT u.id AS usuario, u.correo, u.rol, x.id AS expediente, x.estado,
       (SELECT count(*) FROM plan p WHERE p.expediente_id = x.id) AS planes,
       (SELECT count(*) FROM evaluacion e JOIN plan p ON p.id = e.plan_id WHERE p.expediente_id = x.id) AS evaluaciones
FROM usuario u
LEFT JOIN expediente x ON x.usuario_id = u.id AND x.tipo = 'B'
WHERE u.correo = 'elproo16399@gmail.com';
