package com.dpgil.pathlinker.path_linker.internal;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;

import org.junit.Before;
import org.junit.Test;

import com.dpgil.pathlinker.path_linker.internal.Algorithms.Path;

/** JUnit Test class for the PathLinker */
public class PathLinkerModelTest {

	/** URL object for accessing the input and output files */
	private static URL url;
	/** File object for accessing the input and ouput files */
	private static File file;

	/** object for creating setting up empty network */
	private static NetworkTestSupport support;
	/** original network of directed graph */
	private static CyNetwork originalNetworkDir;
	/** original network of undirected graph */
	private static CyNetwork originalNetworkUndir;
	/** original network of mixed graph */
	private static CyNetwork originalNetworkMixed;

	/** model for running the algorithms on different networks */
	private PathLinkerModel testModel;
	/** Whether or not to generate a subgraph */
	private boolean generateSubgraph;
	/** original test input strings that contains sources */
	private String source;
	/** original test input strings that contains targets */
	private String target;
	/** The k value to be used in the algorithm */
	private int k;
	/** The value by which to penalize each edge weight */
	private double edgePenalty;

	/** map that stores node keys to avoid same nodes added to the network multiple times */
	private static HashMap<String, Long> originalNetworkMap;
	/** list that stores the correct output from output files */
	private ArrayList<String> ans;
	/** list that stores the output computed from the runKSP method */
	private static ArrayList<String> resultDir;
	/** list that stores the undirected network output computed from the runKSP method */
	private static ArrayList<String> resultUndir;
	/** list that stores the mixed network output computed from the runKSP method */
	private static ArrayList<String> resultMixed;


	/**
	 * Creates an empty network with edge_weight as an attribute for edge table
	 * @throws IOException 
	 */
	@Before
	public void setUp() throws IOException {
		//setting up the general required objects for each test cases
		support = new NetworkTestSupport();
		setUpNetwork();

		//setting up the required variables for generating the result (same throughout the test cases)
		generateSubgraph = false;
		source = "P35968 P00533 Q02763";
		target = "Q15797 Q14872 Q16236 P14859 P36956";
		k = 50;
		edgePenalty = 1;
	}

	/**
	 * Test: To test direct graphs
	 * network file (input file): graph-dir_human-interactome.txt
	 * path file (output file):   graph-dir-output-unweighted_human-interactome.txt
	 * 							  graph-dir-output-additive_human-interactome.txt
	 * 							  graph-dir-output-probabilities_human-interactome.txt
	 * network file source: http://bioinformatics.cs.vt.edu/~murali/supplements/2016-sys-bio-applications-pathlinker/ 
	 * @throws IOException 
	 **/
	@Test
	public void testDir() throws IOException {

		//================================= Test unweighted edge weight setting ====================================
		//access output file
		url = this.getClass().getResource("/output/graph-dir-output-unweighted_human-interactome.txt");
		file = new File(url.getFile());

		//construct the string list based on output file
		@SuppressWarnings("resource")
		BufferedReader readerOutput = new BufferedReader(new FileReader(file));

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		modelSetUp(originalNetworkDir, EdgeWeightSetting.UNWEIGHTED, false); //create the model for algorithm
		resultDir = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultDir);

		assertEquals(ans, resultDir); //test the result

		//================================= Test additive edge weight setting =====================================
		//access output file
		url = this.getClass().getResource("/output/graph-dir-output-additive_human-interactome.txt");
		file = new File(url.getFile());

		readerOutput = new BufferedReader(new FileReader(file)); //construct the string list based on output file

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		modelSetUp(originalNetworkDir, EdgeWeightSetting.ADDITIVE, false); //create the model for algorithm
		resultDir = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultDir);

		assertEquals(ans, resultDir); //test the result

		//================================= Test probabilities edge weight setting =====================================
		//access output file
		url = this.getClass().getResource("/output/graph-dir-output-probabilities_human-interactome.txt");
		file = new File(url.getFile());

		readerOutput = new BufferedReader(new FileReader(file)); //construct the string list based on output file

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		modelSetUp(originalNetworkDir, EdgeWeightSetting.PROBABILITIES, false); //create the model for algorithm
		resultDir = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultDir);

		assertEquals(ans, resultDir); //test the result
	}

	/**
	 * Test: To test undirect graphs
	 * network file (input file): graph-undir_human-interactome.txt
	 * path file (output file):   graph-undir-output-unweighted_human-interactome.txt
	 * 							  graph-undir-output-additive_human-interactome.txt
	 * 							  graph-undir-output-probabilities_human-interactome.txt
	 * network file source: http://bioinformatics.cs.vt.edu/~murali/supplements/2016-sys-bio-applications-pathlinker/ 
	 * @throws IOException 
	 */
	@Test
	public void testUndir() throws IOException {

		//================================= Test unweighted edge weight setting =====================================
		//access output file
		url = this.getClass().getResource("/output/graph-undir-output-unweighted_human-interactome.txt");
		file = new File(url.getFile());

		//construct the string list based on output file
		@SuppressWarnings("resource")
		BufferedReader readerOutput = new BufferedReader(new FileReader(file));

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		modelSetUp(originalNetworkUndir, EdgeWeightSetting.UNWEIGHTED, false); //create the model for algorithm
		resultUndir = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultUndir);

		assertEquals(ans, resultUndir); //test the result

		//================================= Test additive edge weight setting =====================================
		//access output file
		url = this.getClass().getResource("/output/graph-undir-output-additive_human-interactome.txt");
		file = new File(url.getFile());

		readerOutput = new BufferedReader(new FileReader(file)); //construct the string list based on output file

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		modelSetUp(originalNetworkUndir, EdgeWeightSetting.ADDITIVE, false); //create the model for algorithm
		resultUndir = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultUndir);

		assertEquals(ans, resultUndir); //test the result

		//================================= Test probabilities edge weight setting =====================================
		//access output file
		url = this.getClass().getResource("/output/graph-undir-output-probabilities_human-interactome.txt");
		file = new File(url.getFile());

		readerOutput = new BufferedReader(new FileReader(file)); //construct the string list based on output file

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		modelSetUp(originalNetworkUndir, EdgeWeightSetting.PROBABILITIES, false); //create the model for algorithm
		resultUndir = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultUndir);

		assertEquals(ans, resultUndir); //test the result
	}

	/**
	 * Test: To test mixed graphs
	 * network file (input file): graph-mixed_human-interactome.txt
	 * path file (output file):   graph-mixed-output-unweighted_human-interactome.txt
	 * 							  graph-mixed-output-additive_human-interactome.txt
	 * 							  graph-mixed-output-probabilities_human-interactome.txt
	 * network file source: http://bioinformatics.cs.vt.edu/~murali/supplements/2016-sys-bio-applications-pathlinker/ 
	 * @throws IOException 
	 */
	@Test
	public void testMixed() throws IOException {

		//================================= Test unweighted edge weight setting =====================================
		//access output file
		url = this.getClass().getResource("/output/graph-mixed-output-unweighted_human-interactome.txt");
		file = new File(url.getFile());

		//construct the string list based on output file
		@SuppressWarnings("resource")
		BufferedReader readerOutput = new BufferedReader(new FileReader(file));

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		modelSetUp(originalNetworkMixed, EdgeWeightSetting.UNWEIGHTED, false); //create the model for algorithm
		resultMixed = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultMixed);

		assertEquals(ans, resultMixed); //test the result

		//================================= Test additive edge weight setting =====================================
		//access output file
		url = this.getClass().getResource("/output/graph-mixed-output-additive_human-interactome.txt");
		file = new File(url.getFile());

		readerOutput = new BufferedReader(new FileReader(file)); //construct the string list based on output file

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		modelSetUp(originalNetworkMixed, EdgeWeightSetting.ADDITIVE, false); //create the model for algorithm
		resultMixed = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultMixed);

		assertEquals(ans, resultMixed); //test the result

		//================================= Test probabilities edge weight setting =====================================
		//access output file
		url = this.getClass().getResource("/output/graph-mixed-output-probabilities_human-interactome.txt");
		file = new File(url.getFile());

		readerOutput = new BufferedReader(new FileReader(file)); //construct the string list based on output file

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		modelSetUp(originalNetworkMixed, EdgeWeightSetting.PROBABILITIES, false); //create the model for algorithm
		resultMixed = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultMixed);

		assertEquals(ans, resultMixed); //test the result
	}

	/**
	 * Test: to test that undir graph and dir graph produces different results given the same input
	 * @throws IOException
	 */
	@Test
	public void testDiff() throws IOException {

		//construct paths for direct graph
		modelSetUp(originalNetworkDir, EdgeWeightSetting.PROBABILITIES, false); //create the model for algorithm
		resultDir = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list
		Collections.sort(resultDir); //sort the lists before comparison

		//construct paths for undirect graph
		modelSetUp(originalNetworkUndir, EdgeWeightSetting.PROBABILITIES, false); //create the model for algorithm
		resultUndir = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list
		Collections.sort(resultUndir); //sort the lists before comparison

		assertThat(resultDir, not(resultUndir));
	}

	/**
	 * Test: to test that the option allow source and target in paths produce the correct result
	 * network file (input file): graph-dir_human-interactome.txt
	 * path file (output file):   graph-dir-output-allowSourceTartgetInPaths_human-interactome.txt
	 * network file source: http://bioinformatics.cs.vt.edu/~murali/supplements/2016-sys-bio-applications-pathlinker/
	 * @throws IOException
	 */
	@Test
	public void testAllowSourceTargetInPaths() throws IOException {
		//access output file
		url = this.getClass().getResource("/output/graph-dir-output-allowSourceTartgetInPaths_human-interactome.txt");
		file = new File(url.getFile());

		//construct the string list based on output file
		@SuppressWarnings("resource")
		BufferedReader readerOutput = new BufferedReader(new FileReader(file));

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		modelSetUp(originalNetworkDir, EdgeWeightSetting.PROBABILITIES, true); //create the model for algorithm
		resultDir = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultDir);

		assertEquals(ans, resultDir); //test the result
	}

	/**
	 * Test: to test that the option of targets are dientical to sources produce the correct result
	 * network file (input file): graph-dir_human-interactome.txt
	 * path file (output file):   graph-dir-output-targetSourceIdentical_human-interactome.txt
	 * network file source: http://bioinformatics.cs.vt.edu/~murali/supplements/2016-sys-bio-applications-pathlinker/
	 * @throws IOException
	 */
	@Test
	public void testTargetSourceIdentical () throws IOException {
		//access output file
		url = this.getClass().getResource("/output/graph-dir-output-targetSourceIdentical_human-interactome.txt");
		file = new File(url.getFile());

		//construct the string list based on output file
		@SuppressWarnings("resource")
		BufferedReader readerOutput = new BufferedReader(new FileReader(file));

		ans = new ArrayList<String>(); //create a new ans list for the output
		for (String line = readerOutput.readLine(); line != null; line = readerOutput.readLine()) {
			String[] arr = line.split("\\s+");
			ans.add(arr[0] + " " + arr[1] + " " + arr[2]);
		}

		testModel = new PathLinkerModel(originalNetworkDir, true, false, 
				source, source, k, EdgeWeightSetting.PROBABILITIES, edgePenalty); //construct model
		testModel.prepareIdSourceTarget();

		resultDir = pathListToStringList(testModel.runKSP()); //construct list of paths as string to compare with ans list

		//sort the lists before comparison
		Collections.sort(ans);
		Collections.sort(resultDir);

		assertEquals(ans, resultDir); //test the result
	}

	/**
	 * Sets up the required networks for testing
	 * @throws IOException 
	 */
	private static void setUpNetwork() throws IOException {

		//================================= setting up direct network =====================================
		originalNetworkMap = new HashMap<String, Long>();
		originalNetworkDir = support.getNetwork();
		originalNetworkDir.getDefaultEdgeTable().createColumn("edge_weight", Double.class, false);

		//access input file
		url = PathLinkerModelTest.class.getResource("/input/graph-dir_human-interactome.txt");
		file = new File(url.getFile());

		//construct network based on input file
		@SuppressWarnings("resource")
		BufferedReader readerInput = new BufferedReader(new FileReader(file));

		for (String line = readerInput.readLine(); line != null; line = readerInput.readLine()) {
			String[] arr = line.split("\\s+");
			CyNode source, target;

			//to avoid adding same source
			if (!originalNetworkMap.containsKey(arr[0])) {
				source = originalNetworkDir.addNode();
				originalNetworkDir.getRow(source).set(CyNetwork.NAME, arr[0]);
				originalNetworkMap.put(arr[0], source.getSUID());
			} else source = originalNetworkDir.getNode(originalNetworkMap.get(arr[0]));

			//to avoid adding same target
			if (!originalNetworkMap.containsKey(arr[1])) {
				target = originalNetworkDir.addNode();
				originalNetworkDir.getRow(target).set(CyNetwork.NAME, arr[1]);
				originalNetworkMap.put(arr[1], target.getSUID());
			} else target = originalNetworkDir.getNode(originalNetworkMap.get(arr[1]));

			CyEdge edge = originalNetworkDir.addEdge(source, target, true); //set true because the the graph is directed
			originalNetworkDir.getRow(edge).set("edge_weight", Double.parseDouble(arr[2]));
		}

		//================================= setting up undirect network =====================================
		originalNetworkMap = new HashMap<String, Long>();
		originalNetworkUndir = support.getNetwork();
		originalNetworkUndir.getDefaultEdgeTable().createColumn("edge_weight", Double.class, false);

		//access input file
		url = PathLinkerModelTest.class.getResource("/input/graph-undir_human-interactome.txt");
		file = new File(url.getFile());

		//construct network based on input file
		readerInput = new BufferedReader(new FileReader(file));

		for (String line = readerInput.readLine(); line != null; line = readerInput.readLine()) {
			String[] arr = line.split("\\s+");
			CyNode source, target;

			//to avoid adding same source
			if (!originalNetworkMap.containsKey(arr[0])) {
				source = originalNetworkUndir.addNode();
				originalNetworkUndir.getRow(source).set(CyNetwork.NAME, arr[0]);
				originalNetworkMap.put(arr[0], source.getSUID());
			} else source = originalNetworkUndir.getNode(originalNetworkMap.get(arr[0]));

			//to avoid adding same target
			if (!originalNetworkMap.containsKey(arr[1])) {
				target = originalNetworkUndir.addNode();
				originalNetworkUndir.getRow(target).set(CyNetwork.NAME, arr[1]);
				originalNetworkMap.put(arr[1], target.getSUID());
			} else target = originalNetworkUndir.getNode(originalNetworkMap.get(arr[1]));

			CyEdge edge = originalNetworkUndir.addEdge(source, target, true);
			originalNetworkUndir.getRow(edge).set("edge_weight", Double.parseDouble(arr[2]));
		}

		//================================= setting up mixed network =====================================
		originalNetworkMap = new HashMap<String, Long>();
		originalNetworkMixed = support.getNetwork();
		originalNetworkMixed.getDefaultEdgeTable().createColumn("edge_weight", Double.class, false);

		//access input file
		url = PathLinkerModelTest.class.getResource("/input/graph-mixed_human-interactome.txt");
		file = new File(url.getFile());

		//construct network based on input file
		readerInput = new BufferedReader(new FileReader(file));

		for (String line = readerInput.readLine(); line != null; line = readerInput.readLine()) {
			String[] arr = line.split("\\s+");
			CyNode source, target;

			//to avoid adding same source
			if (!originalNetworkMap.containsKey(arr[0])) {
				source = originalNetworkMixed.addNode();
				originalNetworkMixed.getRow(source).set(CyNetwork.NAME, arr[0]);
				originalNetworkMap.put(arr[0], source.getSUID());
			} else source = originalNetworkMixed.getNode(originalNetworkMap.get(arr[0]));

			//to avoid adding same target
			if (!originalNetworkMap.containsKey(arr[1])) {
				target = originalNetworkMixed.addNode();
				originalNetworkMixed.getRow(target).set(CyNetwork.NAME, arr[1]);
				originalNetworkMap.put(arr[1], target.getSUID());
			} else target = originalNetworkMixed.getNode(originalNetworkMap.get(arr[1]));

			CyEdge edge = originalNetworkMixed.addEdge(source, target, true);
			originalNetworkMixed.getRow(edge).set("edge_weight", Double.parseDouble(arr[2]));
		}
	}

	/**
	 * Sets up the test model before testing
	 */
	private void modelSetUp(CyNetwork network, EdgeWeightSetting edgeWeightSetting, boolean allowSourceTargetInPaths) {
		testModel = new PathLinkerModel(network, allowSourceTargetInPaths, generateSubgraph, 
				source, target, k, edgeWeightSetting, edgePenalty); //construct model
		testModel.prepareIdSourceTarget();
	}

	/**
	 * Converts a path to a string concatenating the node names A path in the
	 * network involving A -> B -> C would return A|B|C
	 * @param p the path to convert to a string
	 * @return the concatenation of the node names
	 */
	private String pathAsString(Path p) {
		//builds the path string without supersource/supertarget [1,len-1]
		StringBuilder currPath = new StringBuilder();
		for (int i = 1; i < p.size() - 1; i++)
			currPath.append(testModel.getOriginalNetwork().getRow(p.get(i)).get(CyNetwork.NAME, String.class) + "|");

		currPath.setLength(currPath.length() - 1);

		return currPath.toString();
	}

	/**
	 * Converts a list of paths into a list of string
	 * @param p list of paths
	 * @return list of string
	 */
	private ArrayList<String> pathListToStringList(ArrayList<Path> result) {
		ArrayList<String> output = new ArrayList<String>();
		Object[][] rowData = new Object[result.size()][3];

		for (int i = 0; i < result.size(); i++) {
			rowData[i][1] = result.get(i).weight;
			rowData[i][2] = pathAsString(result.get(i));
		}

		// sort the paths based on the alphabet
		// if else statement for different edge weight settings
		if (testModel.getEdgeWeightSetting() == EdgeWeightSetting.PROBABILITIES)
			Arrays.sort(rowData, new Comparator<Object[]>() {
				@Override
				public int compare(Object[] o1, Object[] o2) {
					return Double.compare((double)o1[1], (double)o2[1]) == 0 ? 
							(String.valueOf(o1[2])).compareTo(String.valueOf(o2[2])) : Double.compare((double)o2[1], (double)o1[1]);
				}
			});
		else
			Arrays.sort(rowData, new Comparator<Object[]>() {
				@Override
				public int compare(Object[] o1, Object[] o2) {
					return Double.compare((double)o1[1], (double)o2[1]) == 0 ? 
							(String.valueOf(o1[2])).compareTo(String.valueOf(o2[2])) : Double.compare((double)o1[1], (double)o2[1]);
				}
			});

		// adds sorted paths to output
		for (int j = 0; j < result.size(); j++)
			output.add(j + 1 + " " + rowData[j][1] + " " + rowData[j][2]);

		return output;
	}
}