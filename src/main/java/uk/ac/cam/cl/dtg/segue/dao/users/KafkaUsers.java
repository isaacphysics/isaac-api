package uk.ac.cam.cl.dtg.segue.dao.users;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.KafkaStreamsProducer;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by du220 on 19/07/2017.
 */
public class KafkaUsers implements IUserDataManager {

    private PgUsers pgUsers;
    private KafkaStreamsProducer kafkaProducer;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public KafkaUsers(PostgresSqlDb postgresSqlDb,
                      KafkaStreamsProducer kafkaProducer) {

        this.pgUsers = new PgUsers(postgresSqlDb);
        this.kafkaProducer = kafkaProducer;

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }


    @Override
    public RegisteredUser createOrUpdateUser(RegisteredUser user) throws SegueDatabaseException {

        RegisteredUser regUser = pgUsers.createOrUpdateUser(user);

        Map<String, Object> userDetails = new ImmutableMap.Builder<String, Object>()
                .put("user_id", regUser.getId())
                .put("family_name", regUser.getFamilyName())
                .put("given_name", regUser.getGivenName())
                .put("role", regUser.getRole())
                .put("date_of_birth", (regUser.getDateOfBirth() != null) ? regUser.getDateOfBirth() : "")
                .put("gender", (regUser.getGender() != null) ? regUser.getGender() : "")
                .put("registration_date", regUser.getRegistrationDate().getTime())
                .put("school_id", (regUser.getSchoolId() != null) ? regUser.getSchoolId() : "")
                .put("school_other", (regUser.getSchoolOther() != null) ? regUser.getSchoolOther() : "")
                .put("default_level", (regUser.getDefaultLevel() != null) ? regUser.getDefaultLevel() : "")
                .put("email_verification_status", regUser.getEmailVerificationStatus())
                .build();

        Map<String, Object> kafkaLogRecord = new ImmutableMap.Builder<String, Object>()
                .put("user_id", regUser.getId())
                .put("anonymous_user", false)
                .put("event_type", "CREATE_UPDATE_USER")
                .put("event_details", userDetails)
                .put("timestamp", System.currentTimeMillis())
                .build();
        try {
            // producerRecord contains the name of the kafka topic we are publishing to, followed by the message to be sent.
            ProducerRecord producerRecord = new ProducerRecord<String, String>("topic_logged_events", regUser.getId().toString(),
                    objectMapper.writeValueAsString(kafkaLogRecord));

            kafkaProducer.send(producerRecord);

        } catch (KafkaException kex) {
            kex.printStackTrace();
        } catch (JsonProcessingException jex) {

        }

        return regUser;
    }


    @Override
    public RegisteredUser registerNewUserWithProvider(RegisteredUser user, AuthenticationProvider provider, String providerUserId) throws SegueDatabaseException {
        return pgUsers.registerNewUserWithProvider(user, provider, providerUserId);
    }

    @Override
    public boolean hasALinkedAccount(RegisteredUser user) throws SegueDatabaseException {
        return pgUsers.hasALinkedAccount(user);
    }

    @Override
    public List<AuthenticationProvider> getAuthenticationProvidersByUser(RegisteredUser user) throws SegueDatabaseException {
        return pgUsers.getAuthenticationProvidersByUser(user);
    }

    @Override
    public RegisteredUser getByLinkedAccount(AuthenticationProvider provider, String providerUserId) throws SegueDatabaseException {
        return pgUsers.getByLinkedAccount(provider, providerUserId);
    }

    @Override
    public boolean linkAuthProviderToAccount(RegisteredUser user, AuthenticationProvider provider, String providerUserId) throws SegueDatabaseException {
        return pgUsers.linkAuthProviderToAccount(user, provider, providerUserId);
    }

    @Override
    public void unlinkAuthProviderFromUser(RegisteredUser user, AuthenticationProvider provider) throws SegueDatabaseException {
        pgUsers.unlinkAuthProviderFromUser(user, provider);
    }

    @Override
    public RegisteredUser getByLegacyId(String id) throws SegueDatabaseException {
        return pgUsers.getByLegacyId(id);
    }

    @Override
    public RegisteredUser getById(Long id) throws SegueDatabaseException {
        return pgUsers.getById(id);
    }

    @Override
    public RegisteredUser getByEmail(String email) throws SegueDatabaseException {
        return pgUsers.getByEmail(email);
    }

    @Override
    public List<RegisteredUser> findUsers(RegisteredUser prototype) throws SegueDatabaseException {
        return pgUsers.findUsers(prototype);
    }

    @Override
    public List<RegisteredUser> findUsers(List<Long> usersToLocate) throws SegueDatabaseException {
        return pgUsers.findUsers(usersToLocate);
    }

    @Override
    public RegisteredUser getByEmailVerificationToken(String token) throws SegueDatabaseException {
        return pgUsers.getByEmailVerificationToken(token);
    }

    @Override
    public void deleteUserAccount(RegisteredUser userToDelete) throws SegueDatabaseException {
        pgUsers.deleteUserAccount(userToDelete);
    }

    @Override
    public void updateUserLastSeen(RegisteredUser user) throws SegueDatabaseException {
        pgUsers.updateUserLastSeen(user);
    }

    @Override
    public void updateUserLastSeen(RegisteredUser user, Date date) throws SegueDatabaseException {
        pgUsers.updateUserLastSeen(user, date);
    }
}
