package org.mybatis.jpetstore.util;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * Utility class to retrieve secrets from AWS Secrets Manager.
 */
public class SecretsManager {

    private static final SecretsManagerClient client = SecretsManagerClient.create();

    /**
     * Retrieves a secret value from AWS Secrets Manager.
     *
     * @param secretName The name of the secret to retrieve.
     * @return The secret value.
     * @throws RuntimeException if the secret cannot be retrieved.
     */
    public static String getSecret(String secretName) {
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse valueResponse = client.getSecretValue(valueRequest);
            return valueResponse.secretString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve secret " + secretName + " from AWS Secrets Manager", e);
        }
    }
}
