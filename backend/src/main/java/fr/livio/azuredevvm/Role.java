package fr.livio.azuredevvm;

public enum Role {
    ADMIN(Name.ADMIN),
    ADVANCED(Name.ADVANCED),
    BASIC(Name.BASIC);

    private final String role;

    Role(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return role;
    }

    public static class Name {

        public static final String ADMIN = "admin";
        public static final String ADVANCED = "advanced";
        public static final String BASIC = "basic";

    }
}
