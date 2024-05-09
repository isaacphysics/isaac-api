CREATE TABLE anonymous.nspl21_postcodes
(
    pcd    TEXT,
    lsoa21 TEXT
);

COPY anonymous.nspl21_postcodes (pcd, lsoa21)
    FROM '/nspl_postcodes.csv'
    DELIMITER ','
    CSV HEADER;

CREATE TABLE anonymous.imd_deprivation
(
    lsoa_code                                                      TEXT,
    lsoa_name                                                      TEXT,
    lad_code                                                       TEXT,
    lad_name                                                       TEXT,
    imd_score                                                      NUMERIC,
    imd_rank                                                       NUMERIC,
    imd_decile                                                     NUMERIC,
    income_score                                                   NUMERIC,
    income_rank                                                    NUMERIC,
    income_decile                                                  NUMERIC,
    employment_score                                               NUMERIC,
    employment_rank                                                NUMERIC,
    employment_decile                                              NUMERIC,
    education_skills_and_training_score                            NUMERIC,
    education_skills_and_training_rank                             NUMERIC,
    education_skills_and_training_decile                           NUMERIC,
    health_deprivation_and_disability_score                        NUMERIC,
    health_deprivation_and_disability_rank                         NUMERIC,
    health_deprivation_and_disability_decile                       NUMERIC,
    crime_score                                                    NUMERIC,
    crime_rank                                                     NUMERIC,
    crime_decile                                                   NUMERIC,
    barriers_to_housing_and_services_score                         NUMERIC,
    barriers_to_housing_and_services_rank                          NUMERIC,
    barriers_to_housing_and_services_decile                        NUMERIC,
    living_environment_score                                       NUMERIC,
    living_environment_rank                                        NUMERIC,
    living_environment_decile                                      NUMERIC,
    income_deprivation_affecting_children_index_idaci_score_rate   NUMERIC,
    income_deprivation_affecting_children_index_idaci_rank         NUMERIC,
    income_deprivation_affecting_children_index_idaci_decile       NUMERIC,
    income_deprivation_affecting_older_people_idaopi_score_rate    NUMERIC,
    income_deprivation_affecting_older_people_idaopi_rank          NUMERIC,
    income_deprivation_affecting_older_people_idaopi_decile        NUMERIC,
    children_and_young_people_subdomain_score                      NUMERIC,
    children_and_young_people_subdomain_rank                       NUMERIC,
    children_and_young_people_subdomain_decile                     NUMERIC,
    adult_skills_subdomain_score                                   NUMERIC,
    adult_skills_subdomain_rank                                    NUMERIC,
    adult_skills_subdomain_decile                                  NUMERIC,
    geographical_barriers_subdomain_score                          NUMERIC,
    geographical_barriers_subdomain_rank                           NUMERIC,
    geographical_barriers_subdomain_decile                         NUMERIC,
    wider_barriers_subdomain_score                                 NUMERIC,
    wider_barriers_subdomain_rank                                  NUMERIC,
    wider_barriers_subdomain_decile                                NUMERIC,
    indoors_subdomain_score                                        NUMERIC,
    indoors_subdomain_rank                                         NUMERIC,
    indoors_subdomain_decile                                       NUMERIC,
    outdoors_subdomain_score                                       NUMERIC,
    outdoors_subdomain_rank                                        NUMERIC,
    outdoors_subdomain_decile                                      NUMERIC,
    total_population_mid_2015_excluding_prisoners                  NUMERIC,
    dependent_children_aged_0_to_15_mid_2015_excluding_prisoners   NUMERIC,
    population_aged_16_to_59_mid_2015_excluding_prisoners          NUMERIC,
    older_population_aged_60_and_over_mid_2015_excluding_prisoners NUMERIC,
    working_age_population_18_to_59_excluding_prisoners            NUMERIC
);

COPY anonymous.imd_deprivation (lsoa_code, lsoa_name, lad_code, lad_name, imd_score, imd_rank, imd_decile, income_score,
                                income_rank, income_decile, employment_score, employment_rank, employment_decile,
                                education_skills_and_training_score, education_skills_and_training_rank,
                                education_skills_and_training_decile, health_deprivation_and_disability_score,
                                health_deprivation_and_disability_rank, health_deprivation_and_disability_decile,
                                crime_score, crime_rank, crime_decile, barriers_to_housing_and_services_score,
                                barriers_to_housing_and_services_rank, barriers_to_housing_and_services_decile,
                                living_environment_score, living_environment_rank, living_environment_decile,
                                income_deprivation_affecting_children_index_idaci_score_rate,
                                income_deprivation_affecting_children_index_idaci_rank,
                                income_deprivation_affecting_children_index_idaci_decile,
                                income_deprivation_affecting_older_people_idaopi_score_rate,
                                income_deprivation_affecting_older_people_idaopi_rank,
                                income_deprivation_affecting_older_people_idaopi_decile,
                                children_and_young_people_subdomain_score, children_and_young_people_subdomain_rank,
                                children_and_young_people_subdomain_decile, adult_skills_subdomain_score,
                                adult_skills_subdomain_rank, adult_skills_subdomain_decile,
                                geographical_barriers_subdomain_score, geographical_barriers_subdomain_rank,
                                geographical_barriers_subdomain_decile, wider_barriers_subdomain_score,
                                wider_barriers_subdomain_rank, wider_barriers_subdomain_decile, indoors_subdomain_score,
                                indoors_subdomain_rank, indoors_subdomain_decile, outdoors_subdomain_score,
                                outdoors_subdomain_rank, outdoors_subdomain_decile,
                                total_population_mid_2015_excluding_prisoners,
                                dependent_children_aged_0_to_15_mid_2015_excluding_prisoners,
                                population_aged_16_to_59_mid_2015_excluding_prisoners,
                                older_population_aged_60_and_over_mid_2015_excluding_prisoners,
                                working_age_population_18_to_59_excluding_prisoners)
    FROM '/imd_deprivation_data.csv'
    DELIMITER ','
    CSV HEADER;

CREATE TABLE anonymous.users_enhanced AS
SELECT users.*,
       schools.school_name         AS school_name,
       schools.postcode            AS location,
       imd_deprivation.imd_score   AS imd_deprivation_score,
       imd_deprivation.imd_decile   AS imd_deprivation_decile,
       ncce_priority.priority_type AS ncce_priority_type,
       COALESCE(
               users.country_code,
               CASE schools.data_source
                   WHEN 'GOVERNMENT_WAL' THEN 'GB-WLS'
                   WHEN 'GOVERNMENT_SCO' THEN 'GB-SCT'
                   WHEN 'GOVERNMENT_NI' THEN 'GB-NIR'
                   WHEN 'GOVERNMENT_IE' THEN 'GB-IE'
                   WHEN 'GOVERNMENT_UK' THEN 'GB-ENG'
                   END,
               from_connections.mode_country_code,
               CASE connections_schools.data_source
                   WHEN 'GOVERNMENT_WAL' THEN 'GB-WLS'
                   WHEN 'GOVERNMENT_SCO' THEN 'GB-SCT'
                   WHEN 'GOVERNMENT_NI' THEN 'GB-NIR'
                   WHEN 'GOVERNMENT_IE' THEN 'GB-IE'
                   WHEN 'GOVERNMENT_UK' THEN 'GB-ENG'
                   END
       )                           AS country_code_inferred,
       COALESCE(
               users.school_id,
               from_connections.mode_school_id
       )                           AS school_id_inferred
FROM anonymous.users
     -- Use teacher connections to infer school_id and country_code
         LEFT JOIN
     (SELECT user_id_receiving_permission                        AS teacher_id,
             mode() WITHIN GROUP (ORDER BY student.country_code) AS mode_country_code,
             mode() WITHIN GROUP (ORDER BY student.school_id)    AS mode_school_id
      FROM anonymous.user_associations
               JOIN anonymous.users AS student ON user_associations.user_id_granting_permission = student.id
               JOIN anonymous.users AS teacher ON user_associations.user_id_receiving_permission = teacher.id
      GROUP BY teacher_id) AS from_connections ON users.id = from_connections.teacher_id
         LEFT JOIN anonymous.schools_2022 AS schools ON users.school_id = schools.school_id
         LEFT JOIN anonymous.schools_2022 AS connections_schools
                   ON from_connections.mode_school_id = connections_schools.school_id

     -- Add deprivation data
         LEFT JOIN schools_2021_priority AS ncce_priority ON ncce_priority.urn = users.school_id
         LEFT JOIN anonymous.nspl21_postcodes AS postcodes ON schools.postcode = postcodes.pcd
         LEFT JOIN anonymous.imd_deprivation AS imd_deprivation ON postcodes.lsoa21 = imd_deprivation.lsoa_code;
