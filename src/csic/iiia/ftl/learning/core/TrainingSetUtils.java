/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
  
 package csic.iiia.ftl.learning.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import csic.iiia.ftl.base.core.FTKBase;
import csic.iiia.ftl.base.core.FeatureTerm;
import csic.iiia.ftl.base.core.FloatFeatureTerm;
import csic.iiia.ftl.base.core.IntegerFeatureTerm;
import csic.iiia.ftl.base.core.Ontology;
import csic.iiia.ftl.base.core.Path;
import csic.iiia.ftl.base.core.Symbol;
import csic.iiia.ftl.base.utils.FeatureTermException;
import csic.iiia.ftl.base.utils.Pair;
import csic.iiia.ftl.base.utils.Sampler;

// TODO: Auto-generated Javadoc
/**
 * The Class TrainingSetUtils.
 * 
 * @author santi
 */
public class TrainingSetUtils {

	/** The DEBUG. */
	public static int DEBUG = 0;

	/**
	 * Split training set.
	 * 
	 * @param examples
	 *            the examples
	 * @param n
	 *            the n
	 * @param dp
	 *            the dp
	 * @param sp
	 *            the sp
	 * @param dm
	 *            the dm
	 * @param bias
	 *            the bias
	 * @param redundancy
	 *            the redundancy
	 * @return the list
	 * @throws FeatureTermException
	 *             the feature term exception
	 * @throws Exception
	 *             the exception
	 */
	
	public static List<List<FeatureTerm>> splitTrainingSet(Collection<FeatureTerm> examples, int n, Path dp, Path sp, FTKBase dm, double bias, double redundancy)
			throws FeatureTermException, Exception {
		double matrix[] = null;
		double cbias = 0;
		List<FeatureTerm> differentSolutions = Hypothesis.differentSolutions(examples, sp);
		int ns = differentSolutions.size();
		Random r = new Random();

		// Generate the bias matrix:
		// generate an initial matrix as close as possible to the desired bias:
		for (int i = 0; i < 100; i++) {
			double m[] = new double[n * ns];
			double mbias = 0;
			for (int j = 0; j < n * ns; j++)
				m[j] = r.nextFloat();
			for (int j = 0; j < n; j++) {
				double t = 0;
				for (int k = 0; k < ns; k++)
					t += m[k * n + j];
				for (int k = 0; k < ns; k++)
					m[k * n + j] /= t;
			}
			for (int j = 0; j < n; j++) {
				double t = 0;
				for (int k = 0; k < ns; k++)
					t = (m[k * n + j] - (1.0 / ns)) * (m[k * n + j] - (1.0 / ns));
				mbias += Math.sqrt(t);
			}
			mbias /= n;

			if (matrix == null || Math.abs(bias - mbias) < Math.abs(bias - cbias)) {
				matrix = m;
				cbias = mbias;
			}
		}

		if (DEBUG >= 1)
			System.out.println("Desired bias: " + bias);
		if (DEBUG >= 1)
			System.out.println("Initial bias: " + cbias);

		// Adjust matrix to get closer to desired bias:
		for (int i = 0; i < 1000; i++) {
			boolean stop = true;
			double modifiers[] = { 0.5, 0.75, 0.8, 0.9, 1.1, 1.25, 1.5, 2.0 };

			for (double modifier : modifiers) {
				double m[] = new double[n * ns];
				for (int j = 0; j < n * ns; j++)
					m[j] = matrix[j];
				double mbias = 0;
				for (int j = 0; j < n; j++) {
					for (int k = 0; k < ns; k++) {
						m[k * n + j] = ((m[k * n + j] - (1.0 / n)) * modifier) + (1.0 / n);
						if (m[k * n + j] < 0)
							m[k * n + j] = 0;
						if (m[k * n + j] > 1)
							m[k * n + j] = 1;
					}
					double t = 0;
					for (int k = 0; k < ns; k++)
						t += m[k * n + j];
					if (t > 0)
						for (int k = 0; k < ns; k++)
							m[k * n + j] /= t;
				}
				for (int j = 0; j < n; j++) {
					double t = 0;
					for (int k = 0; k < ns; k++)
						t = (m[k * n + j] - (1.0 / ns)) * (m[k * n + j] - (1.0 / ns));
					mbias += Math.sqrt(t);
				}
				mbias /= n;
				if (Math.abs(bias - mbias) < Math.abs(bias - cbias)) {
					// System.out.println(modifier + " -> " + mbias);
					for (int j = 0; j < n * ns; j++)
						matrix[j] = m[j];
					cbias = mbias;
					stop = false;
					/*
					 * for(FeatureTerm s:differentSolutions) { int j = differentSolutions.indexOf(s); List<Double> d =
					 * new LinkedList<Double>(); for(int k = 0;k<n;k++) d.add(matrix[k*ns+j]);
					 * System.out.println("D for " + s.toStringNOOS(dm) + " -> " + d); }
					 */
				}
			}
			if (stop)
				break;
		}

		if (DEBUG >= 1)
			System.out.println("Adjusted bias: " + cbias);

		// Compute how many cases to distribtue according to redundancy:
		int ncases = (int) ((redundancy * (examples.size()) * (n - 1)) + examples.size());
		if (DEBUG >= 1)
			System.out.println("Redundancy " + redundancy + " -> " + ncases);

		// Sample:
		List<List<FeatureTerm>> training_sets = new LinkedList<List<FeatureTerm>>();
		List<Hypothesis> hypotheses = new LinkedList<Hypothesis>();
		List<FeatureTerm> casesToDistribute = new LinkedList<FeatureTerm>();
		HashMap<FeatureTerm, List<Double>> distributions = new HashMap<FeatureTerm, List<Double>>();

		for (FeatureTerm s : differentSolutions) {
			int i = differentSolutions.indexOf(s);
			List<Double> d = new LinkedList<Double>();
			for (int k = 0; k < n; k++)
				d.add(matrix[k * ns + i]);
			distributions.put(s, d);
			if (DEBUG >= 1)
				System.out.println("Distribution for " + s.toStringNOOS(dm) + " -> " + d);
		}

		for (int i = 0; i < n; i++)
			training_sets.add(new LinkedList<FeatureTerm>());
		for (int i = 0; i < ncases; i++) {
			if (casesToDistribute.isEmpty())
				casesToDistribute.addAll(examples);

			FeatureTerm e = casesToDistribute.get(r.nextInt(casesToDistribute.size()));
			FeatureTerm s = e.readPath(sp);

			boolean found = false;
			// First try to assign it to an agent according to the bias:
			for (int j = 0; j < 10; j++) {
				int a = Sampler.weighted(distributions.get(s));
				if (!training_sets.get(a).contains(e)) {
					training_sets.get(a).add(e);
					found = true;
					break;
				}
			}
			// If not possible give it to a random agent which does not have the case:
			if (!found) {
				while (true) {
					int a = Sampler.random(distributions.get(s));
					if (training_sets.get(a).contains(e)) {
						training_sets.get(a).add(e);
						break;
					}
				}
			}
			casesToDistribute.remove(e);
		}
		return training_sets;
	}
	
	public static List<List<FeatureTerm>> splitTrainingSet_argumentation(Collection<FeatureTerm> examples, List<FeatureTerm> different_solutions, Path dp, Path sp, FTKBase dm, int nb_1, int nb_2) throws FeatureTermException{
		
		if(examples.isEmpty()){
			System.out.println("! Empty set ");
			return null;
		}
		
		ArrayList<List<FeatureTerm>> output = new ArrayList<List<FeatureTerm>>();
		
		List<Integer> merged_ext_1 = new ArrayList<Integer>();
		List<Integer> merged_ext_2 = new ArrayList<Integer>();
		List<Integer> to_increment = new ArrayList<Integer>();
		
		Random r = new Random();
		FeatureTerm s1 = null;
		FeatureTerm s2 = null;
		
		if(nb_1 > different_solutions.size() || nb_2 > different_solutions.size()){
			System.out.println("! not enough categories regarding the merges required ");
			return null;
		}
		
		for(int i = 0; i < 3; i++){
			output.add(new ArrayList<FeatureTerm>());
		}
		
		for(int i = 0; i < nb_1; i++){
			Integer n = r.nextInt(different_solutions.size());
			while(merged_ext_1.contains(n)){
				n = r.nextInt(different_solutions.size());
			}
			merged_ext_1.add(n);
		}
		
		for(int i = 0; i < nb_2; i++){
			Integer n = r.nextInt(different_solutions.size());
			while(merged_ext_1.contains(n) || merged_ext_2.contains(n)){
				n = r.nextInt(different_solutions.size());
			}
			merged_ext_2.add(n);
		}
		
		
		for(int i = 0; i < different_solutions.size(); i++){
			to_increment.add(0);
		}
		
		s1 = different_solutions.get(merged_ext_1.get(0));
		s2 = different_solutions.get(merged_ext_2.get(0));
		
		System.out.println("First dataset :");
		for(Integer i : merged_ext_1){
			System.out.println(different_solutions.get(i).toStringNOOS(dm));
		}
		
		System.out.println("generalized in "+ s1.toStringNOOS(dm));
		
		System.out.println("Second dataset :");
		for(Integer i : merged_ext_2){
			System.out.println(different_solutions.get(i).toStringNOOS(dm));
		}
		
		System.out.println("generalized in "+ s2.toStringNOOS(dm));
		
		// For all FeatureTerm
		for(FeatureTerm f : examples){
			
			// Check the solution & where the FeatureTerm should be added
			FeatureTerm current_solution = f.readPath(sp);
			int s_number = different_solutions.indexOf(current_solution);
			int c_to_add = to_increment.get(s_number) % 3;
			
			// If the solution should be added in 1
			if(c_to_add == 0){
				
				// If the solution is a part of merged 1 & If the solution is different than the first solution of merged 1, change the solution
				if(merged_ext_1.contains(s_number) && !current_solution.equals(s1)){
					f.substitute(current_solution, s1);
				}
			
				// put it in 1
				output.get(0).add(f);
		
				// add 1 to the 1st space of to_increment
				to_increment.set(s_number, c_to_add + 1);
			}
			
			// If the solution should be added in 2
			if(c_to_add == 1){
				
				// If the solution is a part of merged 2 && If the solution is different than the first solution of merged 1, change the solution
				if(merged_ext_2.contains(s_number) && !current_solution.equals(s2)){
					f.substitute(current_solution, s2);
				}
		
				// put it in 2
				output.get(1).add(f);
				
				// add 1 to the 2nd space of to_increment
				to_increment.set(s_number, c_to_add + 1);
			}
			
			// Else
			if(c_to_add == 2){
				
				// Put it in 3
				output.get(2).add(f);
				
				// add 1 to the 3rd space of to_increment
				to_increment.set(s_number, c_to_add + 1);
			}
		}
		
		for(List<FeatureTerm> l : output){
			//System.out.println(l.size());
		}
		
		return output;
	}

	/** The Constant ARTIFICIAL_DATASET. */
	public static final int ARTIFICIAL_DATASET = 0;

	/** The Constant ZOOLOGY_DATASET. */
	public static final int ZOOLOGY_DATASET = 1;

	/** The Constant SOYBEAN_DATASET. */
	public static final int SOYBEAN_DATASET = 2;

	/** The Constant DEMOSPONGIAE_503_DATASET. */
	public static final int DEMOSPONGIAE_503_DATASET = 3;

	/** The Constant DEMOSPONGIAE_280_DATASET. */
	public static final int DEMOSPONGIAE_280_DATASET = 4;

	/** The Constant DEMOSPONGIAE_120_DATASET. */
	public static final int DEMOSPONGIAE_120_DATASET = 5;

	/** The Constant TRAINS_DATASET. */
	public static final int TRAINS_DATASET = 6;

	/** The Constant TRAINS_82_DATASET. */
	public static final int TRAINS_82_DATASET = 61;

	/** The Constant TRAINS_900_DATASET. */
	public static final int TRAINS_900_DATASET = 62;

	/** The Constant TRAINS_100_DATASET. */
	public static final int TRAINS_100_DATASET = 63;

	/** The Constant TRAINS_1000_DATASET. */
	public static final int TRAINS_1000_DATASET = 64;

	/** The Constant TRAINS_10000_DATASET. */
	public static final int TRAINS_10000_DATASET = 65;

	/** The Constant TRAINS_100000_DATASET. */
	public static final int TRAINS_100000_DATASET = 66;

	/** The Constant UNCLE_DATASET. */
	public static final int UNCLE_DATASET = 7;

	/** The Constant UNCLE_DATASET_SETS. */
	public static final int UNCLE_DATASET_SETS = 8;

	/** The Constant UNCLE_DATASET_BOTH. */
	public static final int UNCLE_DATASET_BOTH = 9;

	/** The Constant CARS_DATASET. */
	public static final int CARS_DATASET = 10;

	/** The Constant TOXICOLOGY_DATASET_MRATS. */
	public static final int TOXICOLOGY_DATASET_MRATS = 11;

	/** The Constant TOXICOLOGY_DATASET_FRATS. */
	public static final int TOXICOLOGY_DATASET_FRATS = 12;

	/** The Constant TOXICOLOGY_DATASET_MMICE. */
	public static final int TOXICOLOGY_DATASET_MMICE = 13;

	/** The Constant TOXICOLOGY_DATASET_FMICE. */
	public static final int TOXICOLOGY_DATASET_FMICE = 14;

	/** The Constant TOXICOLOGY_OLD_DATASET_MRATS. */
	public static final int TOXICOLOGY_OLD_DATASET_MRATS = 15;

	/** The Constant TOXICOLOGY_OLD_DATASET_FRATS. */
	public static final int TOXICOLOGY_OLD_DATASET_FRATS = 16;

	/** The Constant TOXICOLOGY_OLD_DATASET_MMICE. */
	public static final int TOXICOLOGY_OLD_DATASET_MMICE = 17;

	/** The Constant TOXICOLOGY_OLD_DATASET_FMICE. */
	public static final int TOXICOLOGY_OLD_DATASET_FMICE = 18;

	/** The Constant KR_VS_KP_DATASET. */
	public static final int KR_VS_KP_DATASET = 19;

	/** The Constant FINANCIAL. */
	public static final int FINANCIAL = 20;

	/** The Constant FINANCIAL_NO_TRANSACTIONS. */
	public static final int FINANCIAL_NO_TRANSACTIONS = 21;

	/** The Constant MUTAGENESIS. */
	public static final int MUTAGENESIS = 22;

	/** The Constant MUTAGENESIS_EASY. */
	public static final int MUTAGENESIS_EASY = 23;

	/** The Constant MUTAGENESIS_DISCRETIZED. */
	public static final int MUTAGENESIS_DISCRETIZED = 24;

	/** The Constant MUTAGENESIS_EASY_DISCRETIZED. */
	public static final int MUTAGENESIS_EASY_DISCRETIZED = 25;

	/** The Constant MUTAGENESIS_NOL_DISCRETIZED. */
	public static final int MUTAGENESIS_NOL_DISCRETIZED = 26;

	/** The Constant MUTAGENESIS_EASY_NOL_DISCRETIZED. */
	public static final int MUTAGENESIS_EASY_NOL_DISCRETIZED = 27;

	/** The Constant RIU_STORIES. */
	public static final int RIU_STORIES = 28;
	
	/** The Constant SEAT for agent 1. */
	public static final int SEAT_1 = 29;
	
	/** The Constant SEAT for agent 2. */
	public static final int SEAT_2 = 30;
	
	/** The Constant SEAT for agent 3. */
	public static final int SEAT_TEST = 31;
	
	/** The Constant SEAT for agent 3. */
	public static final int SEAT_ALL = 34;
	
	/** The Constant ZOOLOGY_DATASET_LB */
	public static final int ZOOLOGY_DATASET_LB = 33;

	/**
	 * Load training set.
	 * 
	 * @param DATASET
	 *            the dATASET
	 * @param o
	 *            the o
	 * @param dm
	 *            the dm
	 * @param case_base
	 *            the case_base
	 * @return the training set properties
	 * @throws FeatureTermException
	 *             the feature term exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static TrainingSetProperties loadTrainingSet(int DATASET, Ontology o, FTKBase dm, FTKBase case_base) throws FeatureTermException, IOException {
		TrainingSetProperties ts = new TrainingSetProperties();
		ts.description_path = new Path();
		ts.solution_path = new Path();
		ts.cases = new LinkedList<FeatureTerm>();

		ts.description_path.features.add(new Symbol("description"));
		ts.solution_path.features.add(new Symbol("solution"));

		switch (DATASET) {
		case ARTIFICIAL_DATASET:
			dm.importNOOS("Resources/DATA/artificial-ontology.noos", o);
			dm.importNOOS("Resources/DATA/artificial-dm.noos", o);
			case_base.importNOOS("Resources/DATA/artificial-512.noos", o);

			ts.name = "artificial";
			ts.problem_sort = o.getSort("artificial-data-problem");
			break;
		case ZOOLOGY_DATASET:
			dm.importNOOS("Resources/DATA/zoology-ontology.noos", o);
			dm.importNOOS("Resources/DATA/zoology-dm.noos", o);
			case_base.importNOOS("Resources/DATA/zoology-cases-102_OLD.noos", o);

			ts.name = "zoology";
			ts.problem_sort = o.getSort("zoo-problem");
			break;
			
		case SOYBEAN_DATASET:
			dm.importNOOS("Resources/DATA/soybean-ontology.noos", o);
			dm.importNOOS("Resources/DATA/soybean-dm.noos", o);
			case_base.importNOOS("Resources/DATA/soybean-cases-307.noos", o);

			ts.name = "soybean";
			ts.problem_sort = o.getSort("soybean-problem");
			break;
		case DEMOSPONGIAE_503_DATASET:
			dm.importNOOS("Resources/DATA/sponge-ontology.noos", o);
			dm.importNOOS("Resources/DATA/sponge-dm.noos", o);
			case_base.importNOOS("Resources/DATA/sponge-cases-503.noos", o);

			ts.solution_path.features.add(new Symbol("order"));

			ts.name = "demospongiae";
			ts.problem_sort = o.getSort("sponge-problem");
			break;
		case DEMOSPONGIAE_280_DATASET:
			dm.importNOOS("Resources/DATA/sponge-ontology.noos", o);
			dm.importNOOS("Resources/DATA/sponge-dm.noos", o);
			case_base.importNOOS("Resources/DATA/sponge-cases-280.noos", o);

			ts.solution_path.features.add(new Symbol("order"));

			ts.name = "demospongiae";
			ts.problem_sort = o.getSort("sponge-problem");
			break;
		case DEMOSPONGIAE_120_DATASET:
			dm.importNOOS("Resources/DATA/sponge-ontology.noos", o);
			dm.importNOOS("Resources/DATA/sponge-dm.noos", o);
			case_base.importNOOS("Resources/DATA/sponge-cases-120.noos", o);

			ts.solution_path.features.add(new Symbol("order"));

			ts.name = "demospongiae";
			ts.problem_sort = o.getSort("sponge-problem");
			break;
		case TRAINS_DATASET:
			dm.importNOOS("Resources/DATA/trains-ontology.noos", o);
			dm.importNOOS("Resources/DATA/trains-dm.noos", o);
			case_base.importNOOS("Resources/DATA/trains-cases-10.noos", o);

			ts.name = "trains";
			ts.problem_sort = o.getSort("trains-problem");
			break;
		case TRAINS_82_DATASET:
			dm.importNOOS("Resources/DATA/trains-ontology.noos", o);
			dm.importNOOS("Resources/DATA/trains-dm.noos", o);
			case_base.importNOOS("Resources/DATA/trains-cases-82.noos", o);

			ts.name = "trains";
			ts.problem_sort = o.getSort("trains-problem");
			break;
		case TRAINS_900_DATASET:
			dm.importNOOS("Resources/DATA/trains-ontology.noos", o);
			dm.importNOOS("Resources/DATA/trains-dm.noos", o);
			case_base.importNOOS("Resources/DATA/trains-cases-900.noos", o);

			ts.name = "trains";
			ts.problem_sort = o.getSort("trains-problem");
			break;
		case TRAINS_100_DATASET:
			dm.importNOOS("Resources/DATA/trains-ontology.noos", o);
			dm.importNOOS("Resources/DATA/trains-dm.noos", o);
			case_base.importNOOS("Resources/DATA/trains-cases-100.noos", o);

			ts.name = "trains";
			ts.problem_sort = o.getSort("trains-problem");
			break;
		case TRAINS_1000_DATASET:
			dm.importNOOS("Resources/DATA/trains-ontology.noos", o);
			dm.importNOOS("Resources/DATA/trains-dm.noos", o);
			case_base.importNOOS("Resources/DATA/trains-cases-1000.noos", o);

			ts.name = "trains";
			ts.problem_sort = o.getSort("trains-problem");
			break;
		case TRAINS_10000_DATASET:
			dm.importNOOS("Resources/DATA/trains-ontology.noos", o);
			dm.importNOOS("Resources/DATA/trains-dm.noos", o);
			case_base.importNOOS("Resources/DATA/trains-cases-10000.noos", o);

			ts.name = "trains";
			ts.problem_sort = o.getSort("trains-problem");
			break;
		case TRAINS_100000_DATASET:
			dm.importNOOS("Resources/DATA/trains-ontology.noos", o);
			dm.importNOOS("Resources/DATA/trains-dm.noos", o);
			case_base.importNOOS("Resources/DATA/trains-cases-100000.noos", o);

			ts.name = "trains";
			ts.problem_sort = o.getSort("trains-problem");
			break;
		case UNCLE_DATASET:
			dm.importNOOS("Resources/DATA/family-ontology.noos", o);
			dm.importNOOS("Resources/DATA/family-dm.noos", o);
			case_base.importNOOS("Resources/DATA/family-cases-12.noos", o);

			ts.name = "uncle";
			ts.problem_sort = o.getSort("uncle-problem");
			break;
		case UNCLE_DATASET_SETS:
			dm.importNOOS("Resources/DATA/family-ontology.noos", o);
			dm.importNOOS("Resources/DATA/family-dm.noos", o);
			case_base.importNOOS("Resources/DATA/family-cases-12-sets.noos", o);

			ts.name = "uncle";
			ts.problem_sort = o.getSort("uncle-problem");
			break;
		case UNCLE_DATASET_BOTH:
			dm.importNOOS("Resources/DATA/family-ontology.noos", o);
			dm.importNOOS("Resources/DATA/family-dm.noos", o);
			case_base.importNOOS("Resources/DATA/family-cases-12.noos", o);
			case_base.importNOOS("Resources/DATA/family-cases-12-sets.noos", o);

			ts.name = "uncle";
			ts.problem_sort = o.getSort("uncle-problem");
			break;
		case CARS_DATASET:
			dm.importNOOS("Resources/DATA/car-ontology.noos", o);
			dm.importNOOS("Resources/DATA/car-dm.noos", o);
			case_base.importNOOS("Resources/DATA/car-1728.noos", o);

			ts.name = "cars";
			ts.problem_sort = o.getSort("car-problem");
			break;

		case TOXICOLOGY_DATASET_MRATS:
		case TOXICOLOGY_DATASET_FRATS:
		case TOXICOLOGY_DATASET_MMICE:
		case TOXICOLOGY_DATASET_FMICE:
			dm.importNOOS("Resources/DATA/toxic-eva-ontology.noos", o);
			dm.importNOOS("Resources/DATA/toxic-eva-dm.noos", o);
			// case_base.ImportNOOS("Resources/DATA/toxic-eva-filtered-cases-276.noos", o);
			// case_base.ImportNOOS("Resources/DATA/toxic-eva-cases-371.noos", o);
			// case_base.ImportNOOS("Resources/DATA/toxic-eva-fixed-cases-371.noos", o);
			case_base.importNOOS("Resources/DATA/toxic-santi-cases-353.noos", o);

			switch (DATASET) {
			case TOXICOLOGY_DATASET_MRATS:
				ts.solution_path.features.add(new Symbol("m-rats"));
				break;
			case TOXICOLOGY_DATASET_FRATS:
				ts.solution_path.features.add(new Symbol("f-rats"));
				break;
			case TOXICOLOGY_DATASET_MMICE:
				ts.solution_path.features.add(new Symbol("m-mice"));
				break;
			case TOXICOLOGY_DATASET_FMICE:
				ts.solution_path.features.add(new Symbol("f-mice"));
				break;
			}

			ts.name = "toxicology";
			ts.problem_sort = o.getSort("toxic-problem");

			{
				List<FeatureTerm> cs = new LinkedList<FeatureTerm>();
				List<FeatureTerm> toDelete = new LinkedList<FeatureTerm>();
				cs.addAll(case_base.searchFT(ts.problem_sort));

				for (FeatureTerm c : cs) {
					FeatureTerm s = c.readPath(ts.solution_path);
					String ss = s.toStringNOOS(dm);
					if (!ss.equals("positive") && !ss.equals("negative")) {
						// remove example, inqdequate!
						case_base.deleteFT(c);
					}
				}

			}
			break;

		case TOXICOLOGY_OLD_DATASET_MRATS:
		case TOXICOLOGY_OLD_DATASET_FRATS:
		case TOXICOLOGY_OLD_DATASET_MMICE:
		case TOXICOLOGY_OLD_DATASET_FMICE:
			dm.importNOOS("Resources/DATA/toxic-eva-old-ontology.noos", o);
			dm.importNOOS("Resources/DATA/toxic-eva-old-dm.noos", o);
			case_base.importNOOS("Resources/DATA/toxic-eva-old-cases.noos", o);

			switch (DATASET) {
			case TOXICOLOGY_OLD_DATASET_MRATS:
				ts.solution_path.features.add(new Symbol("m-rats"));
				break;
			case TOXICOLOGY_OLD_DATASET_FRATS:
				ts.solution_path.features.add(new Symbol("f-rats"));
				break;
			case TOXICOLOGY_OLD_DATASET_MMICE:
				ts.solution_path.features.add(new Symbol("m-mice"));
				break;
			case TOXICOLOGY_OLD_DATASET_FMICE:
				ts.solution_path.features.add(new Symbol("f-mice"));
				break;
			}

			ts.name = "toxicology-old";
			ts.problem_sort = o.getSort("toxic-problem");

			{
				List<FeatureTerm> cs = new LinkedList<FeatureTerm>();
				List<FeatureTerm> toDelete = new LinkedList<FeatureTerm>();
				cs.addAll(case_base.searchFT(ts.problem_sort));

				for (FeatureTerm c : cs) {
					FeatureTerm s = c.readPath(ts.solution_path);
					if (s != null) {
						String ss = s.toStringNOOS(dm);
						if (!ss.equals("positive") && !ss.equals("negative")) {
							// remove example, inqdequate!
							case_base.deleteFT(c);
						}
					}
				}

			}
			break;

		case KR_VS_KP_DATASET:
			dm.importNOOS("Resources/DATA/kr-vs-kp-ontology.noos", o);
			dm.importNOOS("Resources/DATA/kr-vs-kp-dm.noos", o);
			case_base.importNOOS("Resources/DATA/kr-vs-kp-3196.noos", o);

			ts.name = "kr-vs-kp";
			ts.problem_sort = o.getSort("kr-vs-kp-problem");
			break;
		case FINANCIAL_NO_TRANSACTIONS:
			dm.importNOOS("Resources/DATA/financial-ontology.noos", o);
			dm.importNOOS("Resources/DATA/financial-dm.noos", o);
			// case_base.ImportNOOS("Resources/DATA/financial-cases-682-no-transactions.noos", o);
			case_base.importNOOS("Resources/DATA/financial-cases-10-no-transactions.noos", o);

			ts.name = "financial-no-t";
			ts.problem_sort = o.getSort("loan-problem");

			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("loan"));
			ts.solution_path.features.add(new Symbol("status"));
			break;
		case FINANCIAL:
			dm.importNOOS("Resources/DATA/financial-ontology.noos", o);
			dm.importNOOS("Resources/DATA/financial-dm.noos", o);
			case_base.importNOOS("Resources/DATA/financial-cases-10.noos", o);
			// case_base.ImportNOOS("Resources/DATA/financial-cases-682.noos", o);

			ts.name = "financial-no-t";
			ts.problem_sort = o.getSort("loan-problem");

			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("loan"));
			ts.solution_path.features.add(new Symbol("status"));
			break;
		case MUTAGENESIS:
			dm.importNOOS("Resources/DATA/mutagenesis-ontology.noos", o);
			dm.importNOOS("Resources/DATA/mutagenesis-dm.noos", o);
			// case_base.ImportNOOS("Resources/DATA/mutagenesis-b4-230-cases.noos", o);
			case_base.importNOOS("Resources/DATA/mutagenesis-b4-25-cases.noos", o);

			ts.name = "mutagenesis-b4";
			ts.problem_sort = o.getSort("mutagenesis-problem");

			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("problem"));
			ts.solution_path.features.add(new Symbol("solution"));
			break;
		case MUTAGENESIS_EASY:
			dm.importNOOS("Resources/DATA/mutagenesis-ontology.noos", o);
			dm.importNOOS("Resources/DATA/mutagenesis-dm.noos", o);
			case_base.importNOOS("Resources/DATA/mutagenesis-b4-188-cases.noos", o);

			ts.name = "mutagenesis-b4";
			ts.problem_sort = o.getSort("mutagenesis-problem");

			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("problem"));
			ts.solution_path.features.add(new Symbol("solution"));
			break;
		case MUTAGENESIS_DISCRETIZED:
			dm.importNOOS("Resources/DATA/mutagenesis-ontology.noos", o);
			dm.importNOOS("Resources/DATA/mutagenesis-dm.noos", o);
			// case_base.ImportNOOS("Resources/DATA/mutagenesis-b4-230-cases.noos", o);
			case_base.importNOOS("Resources/DATA/mutagenesis-b4-noH-230-cases.noos", o);
			// case_base.ImportNOOS("Resources/DATA/mutagenesis-b4-noH-25-cases.noos", o);

			ts.name = "mutagenesis-b4-discretized";
			ts.problem_sort = o.getSort("mutagenesis-problem");

			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("problem"));
			ts.solution_path.features.add(new Symbol("solution"));

			// discretize:
			{
				Set<FeatureTerm> cases = case_base.searchFT(ts.problem_sort);
				Path fp = new Path();
				fp.features.add(new Symbol("problem"));
				fp.features.add(new Symbol("lumo"));
				TrainingSetUtils.discretizeFeature(cases, fp, ts.solution_path, 2);

				fp.features.clear();
				fp.features.add(new Symbol("problem"));
				fp.features.add(new Symbol("logp"));
				TrainingSetUtils.discretizeFeature(cases, fp, ts.solution_path, 2);
			}

			break;
		case MUTAGENESIS_EASY_DISCRETIZED:
			dm.importNOOS("Resources/DATA/mutagenesis-ontology.noos", o);
			dm.importNOOS("Resources/DATA/mutagenesis-dm.noos", o);
			case_base.importNOOS("Resources/DATA/mutagenesis-b4-188-cases.noos", o);

			ts.name = "mutagenesis-b4-discretized";
			ts.problem_sort = o.getSort("mutagenesis-problem");

			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("problem"));
			ts.solution_path.features.add(new Symbol("solution"));

			// discretize:
			{
				Set<FeatureTerm> cases = case_base.searchFT(ts.problem_sort);
				Path fp = new Path();
				fp.features.add(new Symbol("problem"));
				fp.features.add(new Symbol("lumo"));
				TrainingSetUtils.discretizeFeature(cases, fp, ts.solution_path, 2);

				fp.features.clear();
				fp.features.add(new Symbol("problem"));
				fp.features.add(new Symbol("logp"));
				TrainingSetUtils.discretizeFeature(cases, fp, ts.solution_path, 2);
			}

			break;
		case MUTAGENESIS_EASY_NOL_DISCRETIZED:
			dm.importNOOS("Resources/DATA/mutagenesis-ontology.noos", o);
			dm.importNOOS("Resources/DATA/mutagenesis-dm.noos", o);
			case_base.importNOOS("Resources/DATA/mutagenesis-b4-noH-noL-188-cases.noos", o);

			ts.name = "mutagenesis-b4-nol-discretized";
			ts.problem_sort = o.getSort("mutagenesis-problem");

			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("problem"));
			ts.solution_path.features.add(new Symbol("solution"));

			// discretize:
			{
				Set<FeatureTerm> cases = case_base.searchFT(ts.problem_sort);
				Path fp = new Path();
				fp.features.add(new Symbol("problem"));
				fp.features.add(new Symbol("lumo"));
				TrainingSetUtils.discretizeFeature(cases, fp, ts.solution_path, 2);

				fp.features.clear();
				fp.features.add(new Symbol("problem"));
				fp.features.add(new Symbol("logp"));
				TrainingSetUtils.discretizeFeature(cases, fp, ts.solution_path, 2);
			}
			break;
		case MUTAGENESIS_NOL_DISCRETIZED:
			dm.importNOOS("Resources/DATA/mutagenesis-ontology.noos", o);
			dm.importNOOS("Resources/DATA/mutagenesis-dm.noos", o);
			case_base.importNOOS("Resources/DATA/mutagenesis-b4-noH-noL-230-cases.noos", o);

			ts.name = "mutagenesis-b4-nol-discretized";
			ts.problem_sort = o.getSort("mutagenesis-problem");

			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("problem"));
			ts.solution_path.features.add(new Symbol("solution"));

			// discretize:
			{
				Set<FeatureTerm> cases = case_base.searchFT(ts.problem_sort);
				Path fp = new Path();
				fp.features.add(new Symbol("problem"));
				fp.features.add(new Symbol("lumo"));
				TrainingSetUtils.discretizeFeature(cases, fp, ts.solution_path, 2);

				fp.features.clear();
				fp.features.add(new Symbol("problem"));
				fp.features.add(new Symbol("logp"));
				TrainingSetUtils.discretizeFeature(cases, fp, ts.solution_path, 2);
			}
			break;
		case RIU_STORIES:
			dm.importNOOS("Resources/DATA/story-ontology.noos", o);
			case_base.importNOOS("Resources/DATA/story-cases-2.noos", o);

			ts.name = "riu-stories";
			ts.problem_sort = o.getSort("scene");

			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			break;
		case SEAT_1:
			dm.importNOOS("Resources/seat-ontology.noos", o);
			dm.importNOOS("Resources/seat-dm.noos",o);
			case_base.importNOOS("Resources/seat-cases-learn-1.noos", o);
			
			ts.name = "seat";
			ts.problem_sort = o.getSort("seat-case");
			
			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("description"));
			ts.solution_path.features.add(new Symbol("label"));
			break;
		case SEAT_2:
			dm.importNOOS("Resources/seat-ontology.noos", o);
			dm.importNOOS("Resources/seat-dm.noos",o);
			case_base.importNOOS("Resources/seat-cases-learn-2.noos", o);
			
			ts.name = "seat";
			ts.problem_sort = o.getSort("seat-case");
			
			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("description"));
			ts.solution_path.features.add(new Symbol("label"));
			break;			
		case SEAT_TEST:
			dm.importNOOS("Resources/seat-ontology.noos", o);
			dm.importNOOS("Resources/seat-dm.noos",o);
			case_base.importNOOS("Resources/seat-cases-test.noos", o);
			
			ts.name = "seat";
			ts.problem_sort = o.getSort("seat-case");
			
			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("description"));
			ts.solution_path.features.add(new Symbol("label"));
			break;
			
		case SEAT_ALL:
			dm.importNOOS("Resources/seat-ontology.noos", o);
			dm.importNOOS("Resources/seat-dm.noos",o);
			case_base.importNOOS("Resources/seat-cases-all.noos", o);
			
			ts.name = "seat";
			ts.problem_sort = o.getSort("seat-case");
			
			ts.description_path.features.clear();
			ts.solution_path.features.clear();
			ts.description_path.features.add(new Symbol("description"));
			ts.solution_path.features.add(new Symbol("label"));
			break;
			
		case ZOOLOGY_DATASET_LB:
			dm.importNOOS("Resources/DATA/zoology-ontology2.noos", o);
			dm.importNOOS("Resources/DATA/zoology-dm.noos", o);
			case_base.importNOOS("Resources/DATA/zoology-cases-101.noos", o);

			ts.name = "zoology";
			ts.problem_sort = o.getSort("zoo-problem");
			break;
			
		default:
			return null;
		}

		ts.cases.addAll(case_base.searchFT(ts.problem_sort));
		return ts;
	}

	/**
	 * Discretize feature.
	 * 
	 * @param cases
	 *            the cases
	 * @param featurePath
	 *            the feature path
	 * @param solutionPath
	 *            the solution path
	 * @param ncuts
	 *            the ncuts
	 * @throws FeatureTermException
	 *             the feature term exception
	 */
	public static void discretizeFeature(Collection<FeatureTerm> cases, Path featurePath, Path solutionPath, int ncuts) throws FeatureTermException {
		List<Float> cuts = findDiscretizationIntervals(cases, featurePath, solutionPath, ncuts);

		// change the values by the discretized ones:
		for (FeatureTerm c : cases) {
			FeatureTerm v = c.readPath(featurePath);
			float fv = 0;
			boolean integer = true;
			if (v != null) {
				if (v instanceof IntegerFeatureTerm) {
					fv = ((IntegerFeatureTerm) v).getValue().floatValue();
				} else {
					fv = ((FloatFeatureTerm) v).getValue();
					integer = false;
				}

				int newV = 0;
				for (Float cut : cuts) {
					if (cut < fv)
						newV++;
					else
						break;
				}
				if (integer) {
					((IntegerFeatureTerm) v).setValue(newV);
				} else {
					((FloatFeatureTerm) v).setValue((float) newV);
				}
			} else {
				System.out.println(c.getName() + " has no value in " + featurePath);
			}
		}
	}

	// this method will split the feature range in 2^uts intervals, and return the cut points:
	/**
	 * Find discretization intervals.
	 * 
	 * @param cases
	 *            the cases
	 * @param featurePath
	 *            the feature path
	 * @param solutionPath
	 *            the solution path
	 * @param cuts
	 *            the cuts
	 * @return the list
	 * @throws FeatureTermException
	 *             the feature term exception
	 */
	public static List<Float> findDiscretizationIntervals(Collection<FeatureTerm> cases, Path featurePath, Path solutionPath, int cuts)
			throws FeatureTermException {
		List<Pair<Float, Integer>> values = new LinkedList<Pair<Float, Integer>>();
		Vector<FeatureTerm> solutions = new Vector<FeatureTerm>();

		for (FeatureTerm c : cases) {
			FeatureTerm s = c.readPath(solutionPath);
			if (!solutions.contains(s))
				solutions.add(s);
		}

		// get all the values:
		for (FeatureTerm c : cases) {
			FeatureTerm v = c.readPath(featurePath);
			FeatureTerm s = c.readPath(solutionPath);
			Float fv = null;

			if (v != null) {
				if (v instanceof IntegerFeatureTerm) {
					fv = (((IntegerFeatureTerm) v).getValue()).floatValue();
				} else if (v instanceof FloatFeatureTerm) {
					fv = ((FloatFeatureTerm) v).getValue();
				} else {
					throw new FeatureTermException("The feature has a non numeric value!");
				}
				values.add(new Pair<Float, Integer>(fv, solutions.indexOf(s)));
			}
		}

		// sort them:
		{
			boolean change = false;
			int len = values.size();
			do {
				change = false;
				for (int i = 0; i < len - 1; i++) {
					if (values.get(i).mA > values.get(i + 1).mA) {
						Pair<Float, Integer> tmp = values.get(i);
						values.set(i, values.get(i + 1));
						values.set(i + 1, tmp);
						change = true;
					}
				}
			} while (change);
		}

		// for(Pair<Float,Integer> v:values) {
		// System.out.println(v.m_b + " - " + v.m_a);
		// }

		return discretizeFeatureInternal(values, solutions.size(), cuts);
	}

	/**
	 * Discretize feature internal.
	 * 
	 * @param values
	 *            the values
	 * @param nSolutions
	 *            the n solutions
	 * @param cuts
	 *            the cuts
	 * @return the list
	 */
	static List<Float> discretizeFeatureInternal(List<Pair<Float, Integer>> values, int nSolutions, int cuts) {
		if (cuts == 0) {
			return new LinkedList<Float>();
		} else {
			boolean first = true;
			float bestCut = 0, bestE = 0;

			// System.out.println("discretizing " + values.size() + " values");

			int gDistribution[] = new int[nSolutions];
			float gE = 0;
			for (int i = 0; i < values.size() - 1; i++) {
				float cut = (values.get(i).mA + values.get(i + 1).mA) / 2;

				int d1[] = new int[nSolutions];
				int d2[] = new int[nSolutions];
				int n1 = 0;
				int n2 = 0;

				for (int j = 0; j < values.size(); j++) {
					Pair<Float, Integer> v = values.get(j);
					if (first)
						gDistribution[v.mB]++;
					if (v.mA < cut) {
						d1[v.mB]++;
						n1++;
					} else {
						d2[v.mB]++;
						n2++;
					}
				}

				// compute entropy:
				if (first)
					gE = entropy(gDistribution);
				float e1 = entropy(d1);
				float e2 = entropy(d2);
				float e = (e1 * n1 + e2 * n2) / (float) (n1 + n2);

				if (first || e < bestE) {
					first = false;
					bestE = e;
					bestCut = cut;

					// System.out.println("next best: " + cut + " (" + e + ")");
				}
			}

			if (bestE < gE) {
				List<Float> l = new LinkedList<Float>();

				List<Pair<Float, Integer>> vl1 = new LinkedList<Pair<Float, Integer>>();
				List<Pair<Float, Integer>> vl2 = new LinkedList<Pair<Float, Integer>>();

				for (Pair<Float, Integer> v : values) {
					if (v.mA < bestCut) {
						vl1.add(v);
					} else {
						vl2.add(v);
					}
				}

				l.addAll(discretizeFeatureInternal(vl1, nSolutions, cuts - 1));
				l.add(new Float(bestCut));
				l.addAll(discretizeFeatureInternal(vl2, nSolutions, cuts - 1));
				return l;
			} else {
				return new LinkedList<Float>();
			}
		}
	}

	/**
	 * Entropy.
	 * 
	 * @param hist
	 *            the hist
	 * @return the float
	 */
	static float entropy(int hist[]) {
		int n = hist.length;
		int t = 0;

		for (int i = 0; i < n; i++)
			t += hist[i];

		float h = 0;

		// System.out.print("[ " + hist[0] + "," + hist[1] + "] -> ");

		for (int i = 0; i < n; i++) {
			if (hist[i] != 0) {
				float f = (float) hist[i] / (float) (t);
				h -= Math.log(f) * f;
			}
		}
		// System.out.println("" + h);
		return h;
	}

}
