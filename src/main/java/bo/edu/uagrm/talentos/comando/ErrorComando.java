package bo.edu.uagrm.talentos.comando;

public final class ErrorComando extends RuntimeException {
    private final String codigo;

    public ErrorComando(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
