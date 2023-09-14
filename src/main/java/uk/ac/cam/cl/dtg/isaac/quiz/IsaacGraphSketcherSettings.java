/**
 * Copyright 2022 Matthew Trew
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.quiz;

import org.isaacphysics.graphchecker.settings.SettingsWrapper;


/**
 * This class defines some Isaac-specific settings for the Graph Checker. If you are thinking of changing any
 * settings, you should run the tuner (Bluefin) to evaluate them on the sample set first.
 */
public class IsaacGraphSketcherSettings implements SettingsWrapper {

  private static final double ISAAC_AXIS_SLOP = 0.0025;
  private static final double ISAAC_ORIGIN_SLOP = 0.01;

  @Override
  public double getAxisSlop() {
    return ISAAC_AXIS_SLOP;
  }

  @Override
  public double getOriginSlop() {
    return ISAAC_ORIGIN_SLOP;
  }
}
