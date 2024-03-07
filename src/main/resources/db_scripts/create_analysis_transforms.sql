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
       schools.name                as school_name,
       schools.postcode            as location,
       imd_deprivation.imd_score   as imd_deprivation_score,
       ncce_priority.priority_type as ncce_priority_type,
       COALESCE(
               users.country_code,
               CASE schools.data_source
                   WHEN 'GOVERNMENT_WAL' THEN 'GB-WLS'
                   WHEN 'GOVERNMENT_SCO' THEN 'GB-SCT'
                   WHEN 'GOVERNMENT_NIR' THEN 'GB-NIR'
                   WHEN 'GOVERNMENT_UK' THEN 'GB-ENG'
                   END)
                                   AS country_code_inferred
FROM anonymous.users as users
         left join school_csv_list as schools on schools.urn = users.school_id
         left join schools_2021_priority as ncce_priority on ncce_priority.urn = users.school_id
         left join anonymous.nspl21_postcodes as postcodes on schools.postcode = postcodes.pcd
         left join anonymous.imd_deprivation as imd_deprivation on postcodes.lsoa21 = imd_deprivation.lsoa_code;
