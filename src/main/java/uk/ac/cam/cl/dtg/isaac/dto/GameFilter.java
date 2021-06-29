/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

import com.google.api.client.util.Lists;

/**
 * Object to represent the filter used to generate a particular gameboard.
 * 
 */
public class GameFilter {
    private List<String> subjects;
    private List<String> fields;
    private List<String> topics;
    private List<Integer> levels;
    private List<String> stages;
    private List<String> difficulties;
    private List<String> examBoards;
    private List<String> concepts;
    private List<String> questionCategories;

    /**
     * Create a default game filter object.
     * 
     */
    public GameFilter() {
        this.subjects = Lists.newArrayList();
        this.fields = Lists.newArrayList();
        this.topics = Lists.newArrayList();
        this.levels = Lists.newArrayList();
        this.stages = Lists.newArrayList();
        this.difficulties = Lists.newArrayList();
        this.examBoards = Lists.newArrayList();
        this.concepts = Lists.newArrayList();
        this.questionCategories = Lists.newArrayList();
    }

    /**
     * Constructor to fully populate the game filter object.
     * 
     * @param subjects
     *            - List of subjects used to get the gameboard
     * @param fields
     *            - List of fields used to get the gameboard
     * @param topics
     *            - List of topics used to get the gameboard
     * @param levels
     *            - List of levels used to get the gameboard
     * @param concepts
     *            - List of concepts used to get the gameboard
     * @param questionCategories
     *            - List of questionCategories to get the gameboard ('problem_solving', 'book', etc.)
     */
    public GameFilter(final List<String> subjects, final List<String> fields, final List<String> topics,
            final List<Integer> levels, final List<String> concepts, final List<String> questionCategories,
            final List<String> stages, final List<String> difficulties, final List<String> examBoards) {
        this.subjects = subjects;
        this.fields = fields;
        this.topics = topics;
        this.levels = levels;
        this.stages = stages;
        this.difficulties = difficulties;
        this.examBoards = examBoards;
        this.concepts = concepts;
        this.questionCategories = questionCategories;
    }

    /**
     * Gets the subjectsList.
     * 
     * @return the subjectsList
     */
    public final List<String> getSubjects() {
        return subjects;
    }

    /**
     * Gets the fieldsList.
     * 
     * @return the fieldsList
     */
    public final List<String> getFields() {
        return fields;
    }

    /**
     * Gets the topicsList.
     * 
     * @return the topicsList
     */
    public final List<String> getTopics() {
        return topics;
    }

    /**
     * Gets the levelsList.
     * 
     * @return the levelsList
     */
    public final List<Integer> getLevels() {
        return levels;
    }

    /**
     * Gets the stagesList.
     *
     * @return the stagesList
     */
    public final List<String> getStages() {
        return stages;
    }

    /**
     * Gets the difficultiesList.
     *
     * @return the difficultiesList
     */
    public final List<String> getDifficulties() {
        return difficulties;
    }

    /**
     * Gets the examBoardsList.
     *
     * @return the examBoardsList
     */
    public final List<String> getExamBoards() {
        return examBoards;
    }

    /**
     * Gets the conceptsList.
     * 
     * @return the conceptsList
     */
    public final List<String> getConcepts() {
        return concepts;
    }

    /**
     * Gets the questionCategories.
     *
     * @return the questionCategories
     */
    public final List<String> getQuestionCategories() {
        return questionCategories;
    }

    /**
     * Sets the subjects.
     * 
     * @param subjects
     *            the subjects to set
     */
    public final void setSubjects(final List<String> subjects) {
        this.subjects = subjects;
    }

    /**
     * Sets the fields.
     * 
     * @param fields
     *            the fields to set
     */
    public final void setFields(final List<String> fields) {
        this.fields = fields;
    }

    /**
     * Sets the topics.
     * 
     * @param topics
     *            the topics to set
     */
    public final void setTopics(final List<String> topics) {
        this.topics = topics;
    }

    /**
     * Sets the levels.
     * 
     * @param levels
     *            the levels to set
     */
    public final void setLevels(final List<Integer> levels) {
        this.levels = levels;
    }

    /**
     * Sets the stages.
     *
     * @param stages
     *            the stages to set
     */
    public final void setStages(final List<String> stages) {
        this.stages = stages;
    }

    /**
     * Sets the difficulties.
     *
     * @param difficulties
     *            the difficulties to set
     */
    public final void setDifficulties(final List<String> difficulties) {
        this.difficulties = difficulties;
    }

    /**
     * Sets the exam boards.
     *
     * @param examBoards
     *            the exam boards to set
     */
    public final void setExamBoards(final List<String> examBoards) {
        this.examBoards = examBoards;
    }

    /**
     * Sets the concepts.
     *
     * @param concepts
     *            the concepts to set
     */
    public final void setConcepts(final List<String> concepts) {
        this.concepts = concepts;
    }

    /**
     * Sets the questionCategories.
     *
     * @param questionCategories
     *            the question categories to set
     */
    public final void setQuestionCategories(final List<String> questionCategories) {
        this.questionCategories = questionCategories;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("subjects: " + subjects + " ");
        sb.append("fields: " + fields + " ");
        sb.append("topics: " + topics + " ");
        sb.append("levels: " + levels + " ");
        sb.append("concepts: " + concepts + " ");
        sb.append("questionCategories: " + questionCategories);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((concepts == null) ? 0 : concepts.hashCode());
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + ((levels == null) ? 0 : levels.hashCode());
        result = prime * result + ((subjects == null) ? 0 : subjects.hashCode());
        result = prime * result + ((topics == null) ? 0 : topics.hashCode());
        result = prime * result + ((questionCategories == null) ? 0 : questionCategories.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GameFilter)) {
            return false;
        }
        GameFilter other = (GameFilter) obj;
        if (concepts == null) {
            if (other.concepts != null) {
                return false;
            }
        } else if (!concepts.equals(other.concepts)) {
            return false;
        }
        if (fields == null) {
            if (other.fields != null) {
                return false;
            }
        } else if (!fields.equals(other.fields)) {
            return false;
        }
        if (levels == null) {
            if (other.levels != null) {
                return false;
            }
        } else if (!levels.equals(other.levels)) {
            return false;
        }
        if (subjects == null) {
            if (other.subjects != null) {
                return false;
            }
        } else if (!subjects.equals(other.subjects)) {
            return false;
        }
        if (topics == null) {
            if (other.topics != null) {
                return false;
            }
        } else if (!topics.equals(other.topics)) {
            return false;
        }
        if (questionCategories == null) {
            return other.questionCategories == null;
        } else {
            return questionCategories.equals(other.questionCategories);
        }
    }

}
