/*
 * Copyright 2016 Ryan Lau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacGraphSketcherQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacGraphSketcherValidator;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

/**
 * Content DO for IsaacGraphSketchingQuestion.
 *
 * Created by hhrl2 on 01/08/2016.
 */
@DTOMapping(IsaacGraphSketcherQuestionDTO.class)
@JsonContentType("isaacGraphSketcherQuestion")
@ValidatesWith(IsaacGraphSketcherValidator.class)
public class IsaacGraphSketcherQuestion extends IsaacQuestionBase {
    private int maxNumCurves;
    private String axisLabelX;
    private String axisLabelY;

    public int getMaxNumCurves() {
        return maxNumCurves;
    }

    public void setMaxNumCurves(int maxNumCurves) {
        this.maxNumCurves = maxNumCurves;
    }

    public String getAxisLabelX() {
        return axisLabelX;
    }

    public String getAxisLabelY() {
        return axisLabelY;
    }

    public void setAxisLabelX(String axisLabelX) {
        this.axisLabelX = axisLabelX;
    }

    public void setAxisLabelY(String axisLabelY) {
        this.axisLabelY = axisLabelY;
    }
}
