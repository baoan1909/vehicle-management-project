public class HashTest {
    public static void main(String[] args) throws Exception {
        String s = "Nội dung";
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println(java.util.HexFormat.of().formatHex(bytes));
    }
}