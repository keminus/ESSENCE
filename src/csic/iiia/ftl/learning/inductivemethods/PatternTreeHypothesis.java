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
  
 package csic.iiia.ftl.learning.inductivemethods;

import java.util.HashMap;

import csic.iiia.ftl.base.core.FTKBase;
import csic.iiia.ftl.base.core.FeatureTerm;
import csic.iiia.ftl.base.utils.FeatureTermException;
import csic.iiia.ftl.learning.core.Hypothesis;
import csic.iiia.ftl.learning.core.Prediction;

// TODO: Auto-generated Javadoc
/**
 * Notice that Predictions generated by PatternTreeHypothesis do not contain justifications.
 * 
 * @author santi
 */
public class PatternTreeHypothesis extends Hypothesis {

	/**
	 * The Class PatternTreeNode.
	 */
	public class PatternTreeNode {

		/** The m_pattern. */
		public FeatureTerm m_pattern;

		/** The m_negative child. */
		public PatternTreeNode m_positiveChild, m_negativeChild;

		/** The m_distribution. */
		public HashMap<FeatureTerm, Integer> m_distribution;

		/**
		 * Instantiates a new pattern tree node.
		 * 
		 * @param pattern
		 *            the pattern
		 */
		public PatternTreeNode(FeatureTerm pattern) {
			m_pattern = pattern;
			m_positiveChild = null;
			m_negativeChild = null;
			m_distribution = new HashMap<FeatureTerm, Integer>();
		}
	}

	/** The m_root. */
	PatternTreeNode m_root = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see csic.iiia.ftl.learning.core.Hypothesis#generatePrediction(csic.iiia.ftl.base.core.FeatureTerm,
	 * csic.iiia.ftl.base.core.FTKBase, boolean)
	 */
	public Prediction generatePrediction(FeatureTerm problem, FTKBase dm, boolean debug) throws FeatureTermException, Exception {
		PatternTreeNode current = m_root;

		while (current != null && current.m_pattern != null) {
			if (current.m_pattern.subsumes(problem)) {
				if (current.m_positiveChild == null)
					break;
				current = current.m_positiveChild;
			} else {
				if (current.m_negativeChild == null)
					break;
				current = current.m_negativeChild;
			}
		}

		if (current == null)
			return null;

		Prediction p = new Prediction();
		p.problem = problem;
		for (FeatureTerm s : current.m_distribution.keySet()) {
			p.solutions.add(s);
			p.support.put(s, current.m_distribution.get(s));
		}
		return p;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see csic.iiia.ftl.learning.core.Hypothesis#size()
	 */
	public int size() {
		return sizeInternal(m_root);
	}

	/**
	 * Size internal.
	 * 
	 * @param node
	 *            the node
	 * @return the int
	 */
	protected int sizeInternal(PatternTreeNode node) {
		int size = 0;

		if (node.m_pattern != null)
			size += 1;
		if (node.m_positiveChild != null)
			size += sizeInternal(node.m_positiveChild);
		if (node.m_negativeChild != null)
			size += sizeInternal(node.m_negativeChild);
		return size;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see csic.iiia.ftl.learning.core.Hypothesis#toString(csic.iiia.ftl.base.core.FTKBase)
	 */
	public String toString(FTKBase dm) {
		String tmp = "PatternTreeHypothesis:\n";

		tmp += toStringInternal(dm, m_root, 0);

		return tmp;
	}

	/**
	 * To string internal.
	 * 
	 * @param dm
	 *            the dm
	 * @param node
	 *            the node
	 * @param depth
	 *            the depth
	 * @return the string
	 */
	protected String toStringInternal(FTKBase dm, PatternTreeNode node, int depth) {
		String tmp = "";
		String indent = "";

		for (int i = 0; i < depth; i++)
			indent += "  ";
		if (node.m_pattern != null) {
			tmp += indent + "pattern: \n" + node.m_pattern.toStringNOOS(dm) + "\n";
			tmp += "distribution: [ ";
		} else {
			tmp += indent + "distribution: [ ";
		}
		for (FeatureTerm s : node.m_distribution.keySet()) {
			tmp += s.toStringNOOS(dm) + " - " + node.m_distribution.get(s) + " ";
		}
		tmp += "]";
		tmp = tmp.replaceAll("\n", "\n" + indent);
		tmp += "\n";

		if (node.m_positiveChild != null) {
			tmp += indent + "positive child:\n";
			tmp += toStringInternal(dm, node.m_positiveChild, depth + 1);
		}

		if (node.m_negativeChild != null) {
			tmp += indent + "negative child:\n";
			tmp += toStringInternal(dm, node.m_negativeChild, depth + 1);
		}
		return tmp;
	}
}
