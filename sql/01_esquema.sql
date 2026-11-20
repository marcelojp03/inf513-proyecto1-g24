BEGIN;
CREATE SCHEMA IF NOT EXISTS banco_talentos_mail;
SET LOCAL search_path TO banco_talentos_mail, pg_catalog;

CREATE TABLE IF NOT EXISTS usuario (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    correo varchar(254) NOT NULL UNIQUE CHECK (correo = lower(correo)),
    rol text NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'RECLUTADOR', 'CANDIDATO')),
    activo boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS empresa (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre varchar(120) NOT NULL UNIQUE,
    correo_informes varchar(254) NOT NULL CHECK (correo_informes = lower(correo_informes))
);

CREATE TABLE IF NOT EXISTS cargo (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id bigint NOT NULL REFERENCES empresa,
    nombre varchar(120) NOT NULL,
    carrera varchar(120) NOT NULL,
    experiencia_meses integer NOT NULL CHECK (experiencia_meses BETWEEN 0 AND 600),
    competencias text[] NOT NULL,
    funciones varchar(500) NOT NULL,
    UNIQUE (empresa_id, nombre)
);

CREATE TABLE IF NOT EXISTS expediente (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id bigint NOT NULL REFERENCES usuario,
    tipo text NOT NULL CHECK (tipo IN ('A', 'B')),
    cargo_id bigint REFERENCES cargo,
    nombre varchar(120),
    carrera varchar(120),
    experiencia_meses integer CHECK (experiencia_meses BETWEEN 0 AND 600),
    competencias text[],
    disponible boolean,
    estado text NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'APTO', 'OBSERVADO', 'NO_APTO')),
    creado_en timestamptz NOT NULL DEFAULT now(),
    CHECK ((tipo = 'A') = (cargo_id IS NULL)),
    UNIQUE (usuario_id, cargo_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS expediente_un_a_por_usuario ON expediente (usuario_id) WHERE tipo = 'A';
CREATE INDEX IF NOT EXISTS expediente_por_cargo ON expediente (cargo_id);

CREATE TABLE IF NOT EXISTS consentimiento (
    expediente_id bigint NOT NULL REFERENCES expediente,
    finalidad text NOT NULL CHECK (finalidad IN ('EVALUACION', 'INFORME_EMPRESA')),
    otorgado boolean NOT NULL,
    destino varchar(254),
    actualizado_en timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (expediente_id, finalidad),
    CHECK ((destino IS NOT NULL) = (otorgado AND finalidad = 'INFORME_EMPRESA'))
);

CREATE TABLE IF NOT EXISTS instrumento (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo varchar(24) NOT NULL,
    version integer NOT NULL CHECK (version > 0),
    nombre varchar(120) NOT NULL,
    categoria varchar(80) NOT NULL,
    consigna varchar(600) NOT NULL,
    UNIQUE (codigo, version)
);

CREATE TABLE IF NOT EXISTS plan (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    expediente_id bigint NOT NULL UNIQUE REFERENCES expediente,
    creado_en timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS evaluacion (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_id bigint NOT NULL REFERENCES plan,
    instrumento_id bigint NOT NULL REFERENCES instrumento,
    respuesta varchar(1200),
    respondido_en timestamptz,
    puntaje numeric(5,2) CHECK (puntaje BETWEEN 0 AND 100),
    observacion varchar(400),
    calificado_en timestamptz,
    UNIQUE (plan_id, instrumento_id),
    CHECK ((respuesta IS NULL) = (respondido_en IS NULL)),
    CHECK ((puntaje IS NULL) = (calificado_en IS NULL)),
    CHECK (puntaje IS NULL OR (respuesta IS NOT NULL AND observacion IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS consolidado (
    expediente_id bigint PRIMARY KEY REFERENCES expediente,
    puntaje numeric(5,2) NOT NULL CHECK (puntaje BETWEEN 0 AND 100),
    regla varchar(20) NOT NULL,
    creado_en timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS evento_expediente (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    expediente_id bigint NOT NULL REFERENCES expediente,
    actor_id bigint NOT NULL REFERENCES usuario,
    operacion varchar(24) NOT NULL,
    detalle varchar(450) NOT NULL,
    creado_en timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS evento_por_expediente ON evento_expediente (expediente_id, id DESC);

CREATE TABLE IF NOT EXISTS informe (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    expediente_id bigint NOT NULL REFERENCES expediente,
    autor_id bigint NOT NULL REFERENCES usuario,
    contenido text NOT NULL,
    creado_en timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS token_acceso (
    usuario_id bigint PRIMARY KEY REFERENCES usuario,
    huella char(64) NOT NULL,
    expira_en timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS correo_entrada (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    huella char(64) NOT NULL UNIQUE,
    comando varchar(24),
    resultado varchar(24) NOT NULL DEFAULT 'PENDIENTE',
    registrado_en timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS correo_salida (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entrada_id bigint NOT NULL REFERENCES correo_entrada,
    tipo text NOT NULL CHECK (tipo IN ('RESPUESTA', 'ACCESO', 'INFORME')),
    destino varchar(254) NOT NULL,
    asunto varchar(120) NOT NULL,
    cuerpo text,
    actor_id bigint REFERENCES usuario,
    rol_actor text CHECK (rol_actor IN ('ADMINISTRADOR', 'RECLUTADOR', 'CANDIDATO')),
    informe_id bigint REFERENCES informe,
    expediente_id bigint REFERENCES expediente,
    estado text NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE', 'ENVIANDO', 'ACEPTADO_SMTP', 'CANCELADO', 'FALLIDO', 'INDETERMINADO')),
    intentos integer NOT NULL DEFAULT 0 CHECK (intentos BETWEEN 0 AND 3),
    proximo_intento timestamptz NOT NULL DEFAULT now(),
    resuelto_en timestamptz,
    CHECK ((informe_id IS NOT NULL) = (tipo = 'INFORME')),
    CHECK (tipo = 'RESPUESTA' OR actor_id IS NOT NULL),
    CHECK (tipo = 'ACCESO' OR estado <> 'PENDIENTE' OR cuerpo IS NOT NULL),
    CHECK (estado IN ('PENDIENTE', 'ENVIANDO') OR cuerpo IS NULL)
);
CREATE INDEX IF NOT EXISTS salida_pendiente ON correo_salida (proximo_intento, id) WHERE estado = 'PENDIENTE';
CREATE UNIQUE INDEX IF NOT EXISTS informe_sin_doble_despacho ON correo_salida (informe_id)
    WHERE estado IN ('PENDIENTE', 'ENVIANDO', 'ACEPTADO_SMTP');

COMMIT;
