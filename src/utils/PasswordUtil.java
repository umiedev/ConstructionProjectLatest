package utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;


public class PasswordUtil {

    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_ITERATIONS   = 65536; 
    private static final int KEY_LENGTH_BITS   = 256;
    private static final String ALGORITHM      = "PBKDF2WithHmacSHA256";


    public static String hashPassword(String plainPassword) {
        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            new SecureRandom().nextBytes(salt);

            byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH_BITS);

            return HASH_ITERATIONS + ":" +
                   Base64.getEncoder().encodeToString(salt) + ":" +
                   Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Could not hash password", e);
        }
    }


    public static boolean verifyPassword(String plainPasswordAttempt, String storedHash) {
        if (plainPasswordAttempt == null || storedHash == null) return false;

        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 3) return false;

            int iterations   = Integer.parseInt(parts[0]);
            byte[] salt      = Base64.getDecoder().decode(parts[1]);
            byte[] expected  = Base64.getDecoder().decode(parts[2]);

            byte[] attempt = pbkdf2(plainPasswordAttempt.toCharArray(), salt, iterations, expected.length * 8);

            return constantTimeEquals(expected, attempt);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    /** Avoids leaking timing information about how many bytes matched. */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    /**
     * Enforces a strong-password policy:
     *   - at least 8 characters
     *   - at least one UPPERCASE letter
     *   - at least one lowercase letter
     *   - at least one digit
     *   - at least one special character
     */
    public static String checkStrength(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number.";
        }
        if (!password.matches(".*[!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>/?`~].*")) {
            return "Password must contain at least one special character (e.g. ! @ # $ %).";
        }
        return null;
    }
}


