package uk.ac.cam.cl.dtg.segue.auth.microsoft;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static java.lang.String.format;

public class PublicKey {
    RSAPublicKey publicKey;

    PublicKey(java.security.PublicKey publicKey) {
        if (!(publicKey instanceof RSAPublicKey)) {
            throw new IllegalArgumentException("Public key is not RSAPublicKey");
        }
        this.publicKey = (RSAPublicKey) publicKey;
    }

    public RSAPublicKey toRSA() {
        return publicKey;
    }

    public String id() {
        return format("key_id_%s", this.publicKey.hashCode());
    }

    public String modulus() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(this.publicKey.getModulus().toByteArray());
    }

    public String exponent() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(this.publicKey.getPublicExponent().toByteArray());
    }
}