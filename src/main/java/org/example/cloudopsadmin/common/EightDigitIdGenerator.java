package org.example.cloudopsadmin.common;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Random;

public class EightDigitIdGenerator implements IdentifierGenerator {

    private final Random random = new SecureRandom();

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        // Generate a random integer between 10000000 and 99999999
        // This ensures strictly 8 digits
        int id = 10000000 + random.nextInt(90000000);
        return (long) id;
    }
}
