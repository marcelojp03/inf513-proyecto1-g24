BEGIN;
SET LOCAL search_path TO banco_talentos_mail, pg_catalog;
DO $$
DECLARE
    v_admin bigint;
    v_cand bigint;
    v_recl bigint;
    v_empresa bigint;
    v_cargo bigint;
    v_tec bigint;
    v_per bigint;
    v_u4 bigint;
    v_u5 bigint;
    v_exp1 bigint;
    v_exp2 bigint;
    v_exp3 bigint;
    v_plan1 bigint;
    v_plan2 bigint;
    v_plan3 bigint;
BEGIN
    IF EXISTS (SELECT 1 FROM empresa WHERE nombre = 'Acme SRL') THEN
        RAISE NOTICE 'Los datos de demostración ya están cargados.';
        RETURN;
    END IF;

    SELECT id INTO v_admin FROM usuario WHERE rol = 'ADMINISTRADOR' ORDER BY id LIMIT 1;
    SELECT id INTO v_cand FROM usuario WHERE correo = 'guzmanluisdavid12@gmail.com';
    SELECT id INTO v_recl FROM usuario WHERE correo = 'mendietaletizia@gmail.com';
    IF v_admin IS NULL OR v_cand IS NULL OR v_recl IS NULL THEN
        RAISE EXCEPTION 'Faltan las cuentas base (administrador, candidato y reclutador).';
    END IF;

    INSERT INTO empresa(nombre, correo_informes) VALUES ('Acme SRL', 'marcelojp03@gmail.com') RETURNING id INTO v_empresa;

    INSERT INTO cargo(empresa_id, nombre, carrera, experiencia_meses, competencias, funciones)
    VALUES (v_empresa, 'Analista de datos', 'Administracion', 12,
            string_to_array('excel;atencion', ';'), 'Registrar, analizar y presentar datos')
    RETURNING id INTO v_cargo;

    INSERT INTO instrumento(codigo, version, nombre, categoria, consigna)
    VALUES ('TEC-01', 1, 'Prueba tecnica demo', 'Aptitud', 'Explica por que elegirias la empresa.')
    RETURNING id INTO v_tec;

    INSERT INTO instrumento(codigo, version, nombre, categoria, consigna)
    VALUES ('PER-01', 1, 'Cuestionario de atencion', 'Atencion', 'Describi como priorizas tareas.')
    RETURNING id INTO v_per;

    INSERT INTO usuario(correo, rol) VALUES ('ana.demo@example.test', 'CANDIDATO')
    ON CONFLICT (correo) DO NOTHING;
    SELECT id INTO v_u4 FROM usuario WHERE correo = 'ana.demo@example.test';

    INSERT INTO usuario(correo, rol) VALUES ('carlos.demo@example.test', 'CANDIDATO')
    ON CONFLICT (correo) DO NOTHING;
    SELECT id INTO v_u5 FROM usuario WHERE correo = 'carlos.demo@example.test';

    INSERT INTO expediente(usuario_id, tipo, cargo_id, nombre, carrera, experiencia_meses, competencias, disponible, estado, creado_en)
    VALUES (v_cand, 'B', v_cargo, 'Luis Guzman', 'Administracion', 18,
            string_to_array('excel;atencion', ';'), true, 'APTO', clock_timestamp() - interval '3 days')
    RETURNING id INTO v_exp1;

    INSERT INTO expediente(usuario_id, tipo, cargo_id, nombre, carrera, experiencia_meses, competencias, disponible, estado, creado_en)
    VALUES (v_u4, 'B', v_cargo, 'Ana Demo', 'Administracion', 36,
            string_to_array('excel;atencion;redaccion', ';'), true, 'APTO', clock_timestamp() - interval '2 days')
    RETURNING id INTO v_exp2;

    INSERT INTO expediente(usuario_id, tipo, cargo_id, nombre, carrera, experiencia_meses, competencias, disponible, estado, creado_en)
    VALUES (v_u5, 'B', v_cargo, 'Carlos Demo', 'Economia', 6,
            string_to_array('excel', ';'), false, 'APTO', clock_timestamp() - interval '1 day')
    RETURNING id INTO v_exp3;

    INSERT INTO consentimiento(expediente_id, finalidad, otorgado, destino) VALUES
        (v_exp1, 'EVALUACION', true, NULL),
        (v_exp1, 'INFORME_EMPRESA', true, 'marcelojp03@gmail.com'),
        (v_exp2, 'EVALUACION', true, NULL),
        (v_exp2, 'INFORME_EMPRESA', true, 'marcelojp03@gmail.com'),
        (v_exp3, 'EVALUACION', true, NULL);

    INSERT INTO plan(expediente_id, creado_en) VALUES (v_exp1, clock_timestamp() - interval '3 days') RETURNING id INTO v_plan1;
    INSERT INTO plan(expediente_id, creado_en) VALUES (v_exp2, clock_timestamp() - interval '2 days') RETURNING id INTO v_plan2;
    INSERT INTO plan(expediente_id, creado_en) VALUES (v_exp3, clock_timestamp() - interval '1 day') RETURNING id INTO v_plan3;

    INSERT INTO evaluacion(plan_id, instrumento_id, respuesta, respondido_en, puntaje, observacion, calificado_en) VALUES
        (v_plan1, v_tec, 'Automatizaria el reporte mensual y explicaria el impacto en decisiones.', clock_timestamp() - interval '2 days', 82, 'Respuesta suficiente y aplicada', clock_timestamp() - interval '2 days'),
        (v_plan1, v_per, 'Priorizo por impacto en el cliente y por plazo comprometido.', clock_timestamp() - interval '2 days', 76, 'Criterio de priorizacion adecuado', clock_timestamp() - interval '2 days'),
        (v_plan2, v_tec, 'Partiria del tablero actual y mediria tiempos de cierre.', clock_timestamp() - interval '1 day', 88, 'Aporta criterio de medicion', clock_timestamp() - interval '1 day'),
        (v_plan2, v_per, 'Primero lo urgente del cliente, luego lo interno.', clock_timestamp() - interval '1 day', 84, 'Orden claro', clock_timestamp() - interval '1 day'),
        (v_plan3, v_tec, 'Usaria una planilla compartida con revision semanal.', clock_timestamp() - interval '12 hours', 64, 'Respuesta basica', clock_timestamp() - interval '12 hours'),
        (v_plan3, v_per, 'Empiezo por lo mas facil para avanzar rapido.', clock_timestamp() - interval '12 hours', 58, 'Priorizacion mejorable', clock_timestamp() - interval '12 hours');

    INSERT INTO consolidado(expediente_id, puntaje, regla, creado_en) VALUES
        (v_exp1, 79, 'DEMO_V1', clock_timestamp() - interval '2 days'),
        (v_exp2, 86, 'DEMO_V1', clock_timestamp() - interval '1 day'),
        (v_exp3, 61, 'DEMO_V1', clock_timestamp() - interval '12 hours');

    INSERT INTO evento_expediente(expediente_id, actor_id, operacion, detalle, creado_en) VALUES
        (v_exp1, v_admin, 'VINCEXP', 'Participacion empresarial creada', clock_timestamp() - interval '3 days'),
        (v_exp1, v_recl, 'CLASEXP', 'APTO | Revision documental conforme', clock_timestamp() - interval '3 days'),
        (v_exp1, v_recl, 'ASIGPLAN', 'Plan asignado con 2 instrumentos', clock_timestamp() - interval '3 days'),
        (v_exp1, v_cand, 'RESPRU', 'Respuestas registradas por el candidato', clock_timestamp() - interval '2 days'),
        (v_exp1, v_recl, 'CALPRU', 'Calificacion manual de 2 pruebas', clock_timestamp() - interval '2 days'),
        (v_exp1, v_recl, 'CONSOL', 'Resultado consolidado por regla DEMO_V1', clock_timestamp() - interval '2 days'),
        (v_exp2, v_admin, 'VINCEXP', 'Participacion empresarial creada', clock_timestamp() - interval '2 days'),
        (v_exp2, v_recl, 'CLASEXP', 'APTO | Perfil verificado', clock_timestamp() - interval '2 days'),
        (v_exp2, v_recl, 'CALPRU', 'Calificacion manual de 2 pruebas', clock_timestamp() - interval '1 day'),
        (v_exp3, v_admin, 'VINCEXP', 'Participacion empresarial creada', clock_timestamp() - interval '1 day'),
        (v_exp3, v_recl, 'CALPRU', 'Calificacion manual de 2 pruebas', clock_timestamp() - interval '12 hours');

    RAISE NOTICE 'Demo cargada: empresa %, cargo %, expedientes %, %, %', v_empresa, v_cargo, v_exp1, v_exp2, v_exp3;
END;
$$;
COMMIT;

SET search_path TO banco_talentos_mail, pg_catalog;
SELECT 'usuarios' AS tabla, count(*) FROM usuario
UNION ALL SELECT 'empresas', count(*) FROM empresa
UNION ALL SELECT 'cargos', count(*) FROM cargo
UNION ALL SELECT 'instrumentos', count(*) FROM instrumento
UNION ALL SELECT 'expedientes', count(*) FROM expediente
UNION ALL SELECT 'planes', count(*) FROM plan
UNION ALL SELECT 'evaluaciones', count(*) FROM evaluacion
UNION ALL SELECT 'consolidados', count(*) FROM consolidado
UNION ALL SELECT 'eventos', count(*) FROM evento_expediente
UNION ALL SELECT 'informes', count(*) FROM informe;

