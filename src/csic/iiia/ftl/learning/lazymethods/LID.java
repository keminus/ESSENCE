/**
 * Copyright (c) 2013, Santiago Ontañón All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution. Neither the name of
 * the IIIA-CSIC nor the names of its contributors may be used to endorse or promote products derived from this software
 * without specific prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
  
 package csic.iiia.ftl.learning.lazymethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import csic.iiia.ftl.base.core.FTKBase;
import csic.iiia.ftl.base.core.FeatureTerm;
import csic.iiia.ftl.base.core.Ontology;
import csic.iiia.ftl.base.core.Path;
import csic.iiia.ftl.base.core.SetFeatureTerm;
import csic.iiia.ftl.base.core.Sort;
import csic.iiia.ftl.base.core.Symbol;
import csic.iiia.ftl.base.core.TermFeatureTerm;
import csic.iiia.ftl.base.utils.FeatureTermException;
import csic.iiia.ftl.base.utils.Pair;
import csic.iiia.ftl.learning.core.InformationMeasurement;
import csic.iiia.ftl.learning.core.Prediction;

// TODO: Auto-generated Javadoc
/**
 * The Class LID.
 */
public class LID {

	/** The Constant HEURISTIC_GAIN. */
	public static final int HEURISTIC_GAIN = 0;

	/** The Constant HEURISTIC_RLDM. */
	public static final int HEURISTIC_RLDM = 1;

	/** The Constant HEURISTIC_ENTROPY. */
	public static final int HEURISTIC_ENTROPY = 2;

	/** The Constant SELECT_MAXIMUM. */
	public static final int SELECT_MAXIMUM = 0;

	/** The Constant SELECT_RANDOM. */
	public static final int SELECT_RANDOM = 1;

	/** The Constant SELECT_RANDOM_PONDERATED. */
	public static final int SELECT_RANDOM_PONDERATED = 2;

	/** The Constant SELECT_MINIMUM. */
	public static final int SELECT_MINIMUM = 3;

	/** The s_rand. */
	static Random s_rand = new Random();

	/**
	 * Predict.
	 * 
	 * @param problem
	 *            the problem
	 * @param cases
	 *            the cases
	 * @param description_path
	 *            the description_path
	 * @param solution_path
	 *            the solution_path
	 * @param o
	 *            the o
	 * @param dm
	 *            the dm
	 * @param heuristic
	 *            the heuristic
	 * @param selection_mode
	 *            the selection_mode
	 * @param min_precedents
	 *            the min_precedents
	 * @return the prediction
	 * @throws FeatureTermException
	 *             the feature term exception
	 */
	public static Prediction predict(FeatureTerm problem, List<FeatureTerm> cases, Path description_path, Path solution_path, Ontology o, FTKBase dm,
			int heuristic, int selection_mode, int min_precedents) throws FeatureTermException {

		Prediction prediction = null;

		FeatureTerm justification = null;

		List<FeatureTerm> different_solutions = new ArrayList<FeatureTerm>();
		List<FeatureTerm> descriptions = new ArrayList<FeatureTerm>();
		List<FeatureTerm> solutions = new ArrayList<FeatureTerm>();

		ArrayList<FeatureTerm> patterns;
		float[] patterns_h;
		HashMap<FeatureTerm, Integer> patterns_coverage = new HashMap<FeatureTerm, Integer>();
		int selected;

		// Compute descriptions and solutions:
		{
			for (FeatureTerm c : cases) {
				descriptions.add(c.readPath(description_path));
				solutions.add(c.readPath(solution_path));
			} // while
		}

		// Compute the list of different solutions:
		{
			Set<FeatureTerm> tmp = new HashSet<FeatureTerm>();
			for (FeatureTerm solution : solutions) {
				tmp.add(solution);
			}
			different_solutions.addAll(tmp);
		}

		patterns = computeAllPathPatterns(problem, o, dm);

		// start with an empty term:
		justification = new TermFeatureTerm((Symbol) null, problem.getSort());

		// LID:

		{
			int i;
			boolean terminate = false;

			while (!terminate) {
				// compute all the partitions generated by the patterns and its heuristics (binary, by subsumption)
				{
					FeatureTerm pattern;
					float h;

					patterns_h = new float[patterns.size()];
					patterns_coverage = new HashMap<FeatureTerm, Integer>();

					// System.out.println("------------------------------------");
					for (i = 0; i < patterns.size(); i++) {
						pattern = patterns.get(i);
						switch (heuristic) {
						case HEURISTIC_GAIN: // Information Gain:
						{
							Pair<Float, Integer> p = InformationMeasurement.h_information_gain(descriptions, solutions, different_solutions, pattern);
							h = p.mA;
							patterns_coverage.put(pattern, p.mB);
						}
							break;
						case HEURISTIC_RLDM: // RLDM:
						{
							Pair<Float, Integer> p = InformationMeasurement.h_rldm(descriptions, solutions, different_solutions, pattern);
							h = 1 - p.mA;
							patterns_coverage.put(pattern, p.mB);
						}
							break;
						case HEURISTIC_ENTROPY: // Entropy:
						{
							Pair<Float, Integer> p = InformationMeasurement.h_entropy(descriptions, solutions, different_solutions, pattern);
							h = p.mA;
							patterns_coverage.put(pattern, p.mB);
						}
							break;
						default:
							patterns_coverage.put(pattern, 0);
							h = 0;
							break;
						} // switch
						patterns_h[i] = h;
					} // for
				}

				// sort patterns:
				{
					int j;
					float f_tmp;
					FeatureTerm ft_tmp;

					for (i = 0; i < patterns.size(); i++) {
						for (j = 0; j < i; j++) {
							if (patterns_h[i] < patterns_h[j]) {
								ft_tmp = patterns.get(i);
								patterns.set(i, patterns.get(j));
								patterns.set(j, ft_tmp);

								f_tmp = patterns_h[i];
								patterns_h[i] = patterns_h[j];
								patterns_h[j] = f_tmp;
							} // if
						} // for
					} // for
				}

				// choose a pattern:
				switch (selection_mode) {
				case SELECT_MAXIMUM:// MAXIMUM:
				{
					float maximum = patterns_h[0];
					selected = 0;

					for (i = 1; i < patterns.size(); i++) {
						if (patterns_h[i] > maximum
								|| (patterns_h[i] == maximum && patterns_coverage.get(patterns.get(i)) > patterns_coverage.get(patterns.get(selected)))) {
							maximum = patterns_h[i];
							selected = i;
						} // if
					} // for
				}
					break;
				case SELECT_RANDOM:// RANDOM:
					selected = s_rand.nextInt(patterns.size());
					break;
				case SELECT_RANDOM_PONDERATED:// RANDOM PONDERATED:
				{
					float h_sum = 0;
					float tmp, accum;

					// printf("{ 0");
					for (i = 0; i < patterns.size(); i++) {
						h_sum += patterns_h[i];
						// printf(" - %g",h_sum);
					} // if

					tmp = s_rand.nextFloat() * h_sum;

					selected = -1;
					accum = 0;
					for (i = 0; selected == -1 && i < patterns.size(); i++) {
						if (accum + patterns_h[i] > tmp) {
							selected = i;
						}
						accum += patterns_h[i];
					} // for

					if (selected == -1) {
						selected = s_rand.nextInt(patterns.size());
					}
				}
					break;
				case SELECT_MINIMUM:// MINIMUM:
				{
					float minimum = patterns_h[0];
					selected = 0;

					for (i = 1; i < patterns.size(); i++) {
						if (patterns_h[i] < minimum
								|| (patterns_h[i] == minimum && patterns_coverage.get(patterns.get(i)) > patterns_coverage.get(patterns.get(selected)))) {
							minimum = patterns_h[i];
							selected = i;
						} // if
					} // for
				}
					break;

				default:
					selected = 0;
					break;
				} // switch

				// Apply pattern:
				if (patterns_coverage.get(patterns.get(selected)) > 0) {

					// Filter precedents:
					{
						List<FeatureTerm> to_delete_d = new LinkedList<FeatureTerm>();
						FeatureTerm des, pattern;

						pattern = patterns.get(selected);

						for (i = 0; i < descriptions.size(); i++) {
							des = descriptions.get(i);
							if (!pattern.subsumes(des)) {
								to_delete_d.add(des);
							} // if
						} // while

						while (!to_delete_d.isEmpty()) {
							des = to_delete_d.remove(0);
							int index = descriptions.indexOf(des);
							descriptions.remove(des);
							solutions.remove(index);
						} // while
					}

					// Recompute the list of different solutions:
					{
						Set<FeatureTerm> tmp = new HashSet<FeatureTerm>();
						different_solutions.clear();
						for (FeatureTerm solution : solutions) {
							tmp.add(solution);
						}
						different_solutions.addAll(tmp);
						if (different_solutions.size() == 1) {
							terminate = true;
						}
					}

					// Expand justification:
					{
						FeatureTerm pointer1, pointer2;
						FeatureTerm ft1, ft2, ft;
						Sort sort;

						pointer1 = justification;
						pointer2 = patterns.get(selected);

						while (pointer2 != null) {
							sort = pointer2.getSort();
							for (Symbol feature : sort.getFeatures()) {
								ft1 = pointer1.featureValue(feature);
								ft2 = pointer2.featureValue(feature);

								if (ft2 != null) {
									if (ft1 == null) {
										((TermFeatureTerm) pointer1).defineFeatureValue(feature, ft2.clone(dm, o));
										pointer2 = null;
									} else {
										if (ft2.isLeaf()) {
											if (ft1.equivalents(ft2)) {
												pointer2 = null;
											} else {
												if (ft1 instanceof SetFeatureTerm) {
													((SetFeatureTerm) ft1).addSetValue(ft2.clone(dm, o));
												} else {
													((TermFeatureTerm) pointer1).removeFeatureValue(feature);

													ft = new SetFeatureTerm();
													((SetFeatureTerm) ft).addSetValue(ft1);
													((SetFeatureTerm) ft).addSetValue(ft2.clone(dm, o));
													((TermFeatureTerm) pointer1).defineFeatureValue(feature, ft);
												}

												pointer2 = null;
											} // if
										} else {

											if (dm.contains(ft1)) {
												((TermFeatureTerm) pointer1).removeFeatureValue(feature);

												ft = new SetFeatureTerm();
												((SetFeatureTerm) ft).addSetValue(ft1);
												((SetFeatureTerm) ft).addSetValue(ft2.clone(dm, o));

												((TermFeatureTerm) pointer1).defineFeatureValue(feature, ft);
												pointer2 = null;
											} else {
												if (ft2.getSort().isSubsort(ft1.getSort())) {
													pointer1 = ft1;
													pointer2 = ft2;
												} else {
													if (ft1 instanceof SetFeatureTerm) {
														((SetFeatureTerm) ft1).addSetValue(ft2.clone(dm, o));
													} else {
														((TermFeatureTerm) pointer1).removeFeatureValue(feature);

														ft = new SetFeatureTerm();
														((SetFeatureTerm) ft).addSetValue(ft1);
														((SetFeatureTerm) ft).addSetValue(ft2.clone(dm, o));
														((TermFeatureTerm) pointer1).defineFeatureValue(feature, ft);
													}
													pointer2 = null;

												} // if
											} // if
										} // if
									} // if
									break;
								} // if
							} // while
						} // while
					}

					// Remove invalid patterns:
					{
						for (i = 0; i < patterns.size(); i++) {
							if (patterns_coverage.get(patterns.get(i)) == 0 || patterns.get(i).subsumes(justification)) {
								patterns.remove(patterns.get(i));
								i--;
							} // if
						} // for
					}

					// System.out.println("LID: remainig patterns: " + patterns.size());

					if (patterns.size() == 0) {
						terminate = true;
					}

				} else {
					terminate = true;
				} // if
			} // while
		}

		// compute final solution:
		prediction = new Prediction(problem);

		{
			HashMap<FeatureTerm, Integer> distribution = new HashMap<FeatureTerm, Integer>();

			for (FeatureTerm solution : different_solutions) {
				distribution.put(solution, 0);
			}
			for (FeatureTerm solution : solutions) {
				distribution.put(solution, distribution.get(solution) + 1);
			} // while

			for (FeatureTerm solution : different_solutions) {
				if (distribution.get(solution) > 0) {
					prediction.solutions.add(solution);
					prediction.justifications.put(solution, justification);
					prediction.support.put(solution, distribution.get(solution));
				} // if
			} // for

		} // if

		return prediction;
	} // predict

	/**
	 * Compute all path patterns.
	 * 
	 * @param problem
	 *            the problem
	 * @param o
	 *            the o
	 * @param dm
	 *            the dm
	 * @return the array list
	 * @throws FeatureTermException
	 *             the feature term exception
	 */
	static ArrayList<FeatureTerm> computeAllPathPatterns(FeatureTerm problem, Ontology o, FTKBase dm) throws FeatureTermException {
		// compute all paths (objects containing a single path with the final value):
		List<Path> paths = new ArrayList<Path>();
		List<List<FeatureTerm>> values = new LinkedList<List<FeatureTerm>>();
		List<List<FeatureTerm>> stack_values = new LinkedList<List<FeatureTerm>>();
		List<Path> stack_path = new LinkedList<Path>();
		List<Symbol> l;
		List<FeatureTerm> l2;
		FeatureTerm ft, ft2;
		Symbol feature;
		Path path, path2;
		List<FeatureTerm> value, value2;
		int max_depth = 4;
		int npaths;

		value = new LinkedList<FeatureTerm>();
		value.add(problem);
		stack_values.add(value);
		stack_path.add(new Path());

		// get all paths:
		while (!stack_values.isEmpty()) {
			value = stack_values.remove(0);
			path = stack_path.remove(0);

			ft = value.get(value.size() - 1);

			if (path.features.size() >= max_depth || ft.isLeaf()) {
				value2 = new LinkedList<FeatureTerm>(value);
				ft2 = value2.remove(0);
				values.add(value2);
				paths.add(new Path(path));
			} else {

				if (ft instanceof TermFeatureTerm) {
					l = ft.getSort().getFeatures();

					while (!l.isEmpty()) {
						feature = l.remove(0);

						ft2 = ft.featureValue(feature);
						if (ft2 != null) {
							path2 = new Path(path);
							path2.features.add(feature);
							value2 = new LinkedList<FeatureTerm>(value);
							value2.add(ft2);
							stack_values.add(value2);
							stack_path.add(path2);
						} // if
					} // while
				} // if

				if (ft instanceof SetFeatureTerm) {
					l2 = ((SetFeatureTerm) ft).getSetValues();

					for (FeatureTerm ft4 : l2) {
						path2 = new Path(path);
						value2 = new LinkedList<FeatureTerm>(value);
						value2.remove(value2.size() - 1);
						value2.add(ft4);
						stack_values.add(value2);
						stack_path.add(path2);
					} // while
				} // if
			} // if
		} // while

		npaths = paths.size();

		// construct the patterns representing the paths:
		Sort sort;
		FeatureTerm pattern, pointer;

		ArrayList<FeatureTerm> patterns = new ArrayList<FeatureTerm>(npaths);

		for (int i = 0; i < paths.size(); i++) {
			path = paths.get(i);
			value = values.get(i);

			sort = problem.getSort();
			if (sort.getDataType() != Sort.DATATYPE_FEATURETERM) {
				throw new FeatureTermException("Problem in LID is not a feature term!");
			}

			pattern = new TermFeatureTerm((Symbol) null, sort);
			pointer = pattern;

			for (int j = 0; j < path.features.size(); j++) {
				feature = path.features.get(j);
				ft = value.get(j);

				if (j < path.features.size() - 1) {
					sort = ft.getSort();
					if (sort.getDataType() != Sort.DATATYPE_FEATURETERM) {
						throw new FeatureTermException("Problem in LID is not a feature term!");
					}
					ft2 = new TermFeatureTerm((Symbol) null, sort);
					((TermFeatureTerm) pointer).defineFeatureValue(feature, ft2);
					pointer = ft2;
				} else {
					((TermFeatureTerm) pointer).defineFeatureValue(feature, ft);
				}
			}

			patterns.add(pattern);
		} // for

		return patterns;
	}
}
