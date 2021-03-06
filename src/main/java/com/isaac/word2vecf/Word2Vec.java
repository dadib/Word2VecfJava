package com.isaac.word2vecf;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import javafx.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by zhanghao on 5/6/17.
 * Modified by zhanghao on 10/10/17.
 * @author  ZHANG HAO
 * email: isaac.changhau@gmail.com
 */
@SuppressWarnings("all")
public class Word2Vec {
	final int layerSize;
	final List<String> wordVocab;
	final INDArray wordVectors;

	public Word2Vec (int layerSize, List<String> wordVocab, INDArray wordVectors, boolean normalize) {
		this.layerSize = layerSize;
		this.wordVocab = wordVocab;
		if (normalize) this.wordVectors = normalize(wordVectors);
		else this.wordVectors = wordVectors;
	}

	/** @return layerSize */
	public int dimension () {
		return layerSize;
	}

	/** @return word vocabulary size */
	public int wordVocabSize () {
		return wordVocab.size();
	}

	/** @return true if it is contained in word vocabulary, false if not */
	public boolean hasWord (String str) {
		return wordVocab.contains(str);
	}

	/** @return {@link INDArray} vector if it is contained in word vocabulary, full-zero vector if not */
	public INDArray getWordVector (String str) { return hasWord(str) ? wordVectors.getRow(wordVocab.indexOf(str)) : zeros(); }

	/** @return mean vector, built from words passed in */
	public INDArray getWordVectorMean (List<String> words) {
		INDArray res = zeros();
		if (words == null || words.isEmpty()) return res;
		for (String word : words) res.addi(getWordVector(word));
		return res.div(Nd4j.scalar(words.size()));
	}

	/** @return the index of given word in word vocabulary, -1 nor found */
	public int wordIndexOf (String word) {
		return wordVocab.indexOf(word);
	}

	/** @return cosine similarity between two given words */
	public double wordSimilarity (String word1, String word2) {
		return getWordVector(word1).mmul(getWordVector(word2).transpose()).getDouble(0);
	}

	/** @return similarity scores between vector and all records in word vectors */
	public INDArray wordSimilarity (INDArray vector) {
		Preconditions.checkArgument(vector.columns() == layerSize, String.format("vector's dimension(%d) must be the same as layer size(%d)", vector.columns(), layerSize));
		return wordVectors.mmul(vector.transpose());
	}

	/** @return list of nearest {@link Pair} of given word, size num, each {@link Pair} contains result word and it cosine score */
	public List<Pair<String, Double>> wordsNearest (String word, Integer top) {
		return wordsNearest(getWordVector(word), top);
	}

	/** @return list of nearest {@link Pair} of given word, size num, each {@link Pair} contains result word and it cosine score */
	public List<Pair<String, Double>> wordsNearest (INDArray word, Integer top) {
		top = MoreObjects.firstNonNull(top, 10); // set default value: 10
		INDArray res = wordSimilarity(word);
		List<Pair<String, Double>> list = new ArrayList<>(wordVocab.size());
		for (int i = 0; i < wordVocab.size(); i++) { list.add(new Pair<>(wordVocab.get(i), res.getDouble(i))); }
		return list.stream().sorted((e1, e2) -> Double.valueOf(e2.getValue()).compareTo(Double.valueOf(e1.getValue())))
				.limit(top).collect(Collectors.toCollection(LinkedList::new));
	}

	/**
	 * Analogy task, "king" - "queen" = "man" - ["woman"], positive words: man, queen; negative words: king; target word: woman.
	 * @param positive list of positive words
	 * @param negative list of negative words
	 * @param top number of results return
	 * @return list of nearest words, size num
	 */
	public List<String> wordsNearest (List<String> positive, List<String> negative, Integer top) {
		top = MoreObjects.firstNonNull(top, 10); // set default value: 10
		INDArray pos = zeros();
		INDArray neg = zeros();
		for (String str : positive) { pos.addiRowVector(getWordVector(str)); }
		for (String str : negative) { neg.addiRowVector(getWordVector(str)); }
		INDArray res = wordSimilarity(pos.sub(neg));
		List<Pair<String, Double>> list = new ArrayList<>(wordVocab.size());
		for (int i = 0; i < wordVocab.size(); i++) { list.add(new Pair<>(wordVocab.get(i), res.getDouble(i))); }
		return list.stream().sorted((e1, e2) -> Double.valueOf(e2.getValue()).compareTo(Double.valueOf(e1.getValue())))
				.limit(top).map(Pair::getKey).collect(Collectors.toCollection(LinkedList::new));
	}

	/** @return {@link INDArray} */
	INDArray normalize(INDArray array) {
		INDArray norm = array.norm2(1);
		for (int i = 0; i < norm.size(0); i++) {
			for (int j = 0; j < norm.size(1); j++) {
				if (norm.getFloat(i) == 0) norm.put(i, j, 1.0);
			}
		}
		return array.diviColumnVector(norm);
	}

	/** @return {@link INDArray} */
	public INDArray zeros () {
		return Nd4j.zeros(1, layerSize);
	}

	/** @return {@link INDArray} */
	public INDArray ones () {
		return Nd4j.ones(1, layerSize);
	}

	public int getLayerSize () { return layerSize; }

	public List<String> getWordVocab () { return wordVocab; }

	public INDArray getWordVectors () { return wordVectors; }
}
