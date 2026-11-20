BEGIN;
SET LOCAL search_path TO banco_talentos_mail, pg_catalog;
DO $$
DECLARE
    v_emp bigint;
    v_c2 bigint;
    v_tec bigint;
    v_per bigint;
    v_admin bigint;
    v_recl bigint;
    v_recl2 bigint;
    v_luis bigint;
    v_ana bigint;
    v_carlos bigint;
    v_a1 bigint;
    v_a2 bigint;
    v_b2 bigint;
    v_b3 bigint;
    v_b4 bigint;
    v_p1 bigint;
    v_p2 bigint;
BEGIN
    IF EXISTS (SELECT 1 FROM cargo WHERE nombre = 'Asistente administrativo') THEN
        RAISE NOTICE 'Los datos de métricas ya están cargados.';
        RETURN;
    END IF;

    SELECT id INTO v_emp FROM empresa WHERE nombre = 'FICCT' ORDER BY id LIMIT 1;
    SELECT id INTO v_tec FROM instrumento WHERE codigo = 'TEC-01' ORDER BY id LIMIT 1;
    SELECT id INTO v_per FROM instrumento WHERE codigo = 'PER-01' ORDER BY id LIMIT 1;
    SELECT id INTO v_admin FROM usuario WHERE rol = 'ADMINISTRADOR' ORDER BY id LIMIT 1;
    SELECT id INTO v_recl FROM usuario WHERE correo = 'mendietaletizia@gmail.com';
    SELECT id INTO v_recl2 FROM usuario WHERE correo = 'mjimenezp126@ficct.uagrm.edu.bo';
    SELECT id INTO v_luis FROM usuario WHERE correo = 'guzmanluisdavid12@gmail.com';
    SELECT id INTO v_ana FROM usuario WHERE correo = 'ana.demo@example.test';
    SELECT id INTO v_carlos FROM usuario WHERE correo = 'carlos.demo@example.test';
    IF v_emp IS NULL OR v_tec IS NULL OR v_per IS NULL OR v_admin IS NULL OR v_recl2 IS NULL
       OR v_luis IS NULL OR v_ana IS NULL OR v_carlos IS NULL THEN
        RAISE EXCEPTION 'Faltan los datos base (02 y 04).';
    END IF;

    INSERT INTO cargo(empresa_id, nombre, carrera, experiencia_meses, competencias, funciones)
    VALUES (v_emp, 'Asistente administrativo', 'Administracion', 6,
            string_to_array('excel;redaccion', ';'), 'Apoyar la gestion documental')
    RETURNING id INTO v_c2;

    INSERT INTO expediente(usuario_id, tipo, cargo_id, nombre, carrera, experiencia_meses, competencias, disponible, estado)
    VALUES (v_ana, 'A', NULL, 'Ana Demo', 'Administracion', 36,
            string_to_array('excel;atencion;redaccion', ';'), true, 'APTO')
    RETURNING id INTO v_a1;

    INSERT INTO consentimiento(expediente_id, finalidad, otorgado, destino)
    VALUES (v_a1, 'EVALUACION', true, NULL);

    INSERT INTO plan(expediente_id) VALUES (v_a1) RETURNING id INTO v_p1;
    INSERT INTO evaluacion(plan_id, instrumento_id, respuesta, respondido_en, puntaje, observacion, calificado_en)
    VALUES (v_p1, v_tec, 'Mediria tiempos de cierre y automatizaria el tablero.', clock_timestamp() - interval '5 days',
            90, 'Criterio de medicion solido', clock_timestamp() - interval '5 days');
    INSERT INTO consolidado(expediente_id, puntaje, regla, creado_en)
    VALUES (v_a1, 90, 'DEMO_V1', clock_timestamp() - interval '5 days');

    INSERT INTO expediente(usuario_id, tipo, cargo_id, nombre, carrera, experiencia_meses, competencias, disponible, estado)
    VALUES (v_carlos, 'A', NULL, 'Carlos Demo', 'Economia', 6,
            string_to_array('excel', ';'), false, 'OBSERVADO')
    RETURNING id INTO v_a2;
    INSERT INTO consentimiento(expediente_id, finalidad, otorgado, destino)
    VALUES (v_a2, 'EVALUACION', true, NULL);

    INSERT INTO expediente(usuario_id, tipo, cargo_id, nombre, carrera, experiencia_meses, competencias, disponible, estado)
    VALUES (v_ana, 'B', v_c2, 'Ana Demo', 'Administracion', 36,
            string_to_array('excel;atencion;redaccion', ';'), true, 'APTO')
    RETURNING id INTO v_b2;
    INSERT INTO consentimiento(expediente_id, finalidad, otorgado, destino) VALUES
        (v_b2, 'EVALUACION', true, NULL),
        (v_b2, 'INFORME_EMPRESA', true, 'marcelojp03@gmail.com');

    INSERT INTO plan(expediente_id) VALUES (v_b2) RETURNING id INTO v_p2;
    INSERT INTO evaluacion(plan_id, instrumento_id, respuesta, respondido_en, puntaje, observacion, calificado_en) VALUES
        (v_p2, v_tec, 'Partiria del tablero actual y mediria el impacto mensual.', clock_timestamp() - interval '4 days',
         92, 'Aporta criterio de medicion', clock_timestamp() - interval '4 days'),
        (v_p2, v_per, 'Primero el cliente, luego lo interno.', clock_timestamp() - interval '4 days',
         88, 'Orden claro', clock_timestamp() - interval '4 days');
    INSERT INTO consolidado(expediente_id, puntaje, regla, creado_en)
    VALUES (v_b2, 90, 'DEMO_V1', clock_timestamp() - interval '4 days');

    INSERT INTO expediente(usuario_id, tipo, cargo_id, nombre, carrera, experiencia_meses, competencias, disponible, estado)
    VALUES (v_carlos, 'B', v_c2, 'Carlos Demo', 'Economia', 6,
            string_to_array('excel', ';'), false, 'OBSERVADO')
    RETURNING id INTO v_b3;
    INSERT INTO consentimiento(expediente_id, finalidad, otorgado, destino)
    VALUES (v_b3, 'EVALUACION', true, NULL);

    INSERT INTO expediente(usuario_id, tipo, cargo_id, nombre, carrera, experiencia_meses, competencias, disponible, estado)
    VALUES (v_luis, 'B', v_c2, 'Luis Guzman', 'Administracion', 18,
            string_to_array('excel;atencion', ';'), true, 'NO_APTO')
    RETURNING id INTO v_b4;
    INSERT INTO consentimiento(expediente_id, finalidad, otorgado, destino)
    VALUES (v_b4, 'EVALUACION', true, NULL);

    INSERT INTO evento_expediente(expediente_id, actor_id, operacion, detalle, creado_en) VALUES
        (v_a1, v_admin, 'VINCEXP', 'Inscripcion universitaria interna', clock_timestamp() - interval '6 days'),
        (v_a1, v_recl, 'CLASEXP', 'APTO | Revisión académica conforme', clock_timestamp() - interval '6 days'),
        (v_a1, v_recl, 'CONSOL', 'Resultado consolidado por regla DEMO_V1', clock_timestamp() - interval '5 days'),
        (v_a2, v_admin, 'VINCEXP', 'Inscripcion universitaria interna', clock_timestamp() - interval '3 days'),
        (v_a2, v_recl2, 'CLASEXP', 'OBSERVADO | Falta documentación de respaldo', clock_timestamp() - interval '3 days'),
        (v_b2, v_recl2, 'CLASEXP', 'APTO | Perfil verificado', clock_timestamp() - interval '4 days'),
        (v_b2, v_recl2, 'ASIGPLAN', 'Plan asignado con 2 instrumentos', clock_timestamp() - interval '4 days'),
        (v_b2, v_recl2, 'CALPRU', 'Calificación manual de 2 pruebas', clock_timestamp() - interval '4 days'),
        (v_b2, v_recl2, 'CONSOL', 'Resultado consolidado por regla DEMO_V1', clock_timestamp() - interval '4 days'),
        (v_b3, v_recl2, 'CLASEXP', 'OBSERVADO | Experiencia insuficiente para el cargo', clock_timestamp() - interval '2 days'),
        (v_b4, v_admin, 'CLASEXP', 'NO_APTO | Perfil no coincide con el cargo', clock_timestamp() - interval '2 days');

    RAISE NOTICE 'Métricas cargadas: cargo %, expedientes A %, % y B %, %, %', v_c2, v_a1, v_a2, v_b2, v_b3, v_b4;
END;
$$;
COMMIT;

SET search_path TO banco_talentos_mail, pg_catalog;
SELECT 'usuarios' AS dato, count(*) FROM usuario
UNION ALL SELECT 'empresas', count(*) FROM empresa
UNION ALL SELECT 'cargos', count(*) FROM cargo
UNION ALL SELECT 'expedientes', count(*) FROM expediente
UNION ALL SELECT 'planes', count(*) FROM plan
UNION ALL SELECT 'evaluaciones', count(*) FROM evaluacion
UNION ALL SELECT 'consolidados', count(*) FROM consolidado
UNION ALL SELECT 'eventos', count(*) FROM evento_expediente
UNION ALL SELECT 'informes', count(*) FROM informe;
