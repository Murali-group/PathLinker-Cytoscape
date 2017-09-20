PathLinker Cytoscape
====================

This software implements the PathLinker algorithm for reconstructing signaling pathways from protein interaction networks as an app for the Cytoscape software. You can download the app on the Cytoscape app store at http://apps.cytoscape.org/apps/pathlinker.

## Overview

PathLinker is a method for reconstructing signalling pathways from protein interaction and regulatory networks. Consider a protein-protein interaction network represented as a directed graph. The network may contain physical, signalling, and regulatory interactions between pairs of proteins. Given a query composed of a set of receptors and a set of transcription factors in this network that represent the "sources" and "targets" of some specific signalling pathway (e.g., the Wnt pathway), PathLinker attempts to compute a high quality reconstruction of this pathway. Specifically, PathLinker computes a sub-network that connects the receptors in the query to the transcription factors in the query. More generally, PathLinker will be useful for connecting any set of sources to any set of targets in an interaction network.

The primary algorithmic component of PathLinker is computing the _k_-shortest simple paths in the network from any source to any target. PathLinker accomplishes this task through a novel integration of Yen's algorithm with the A* heuristic, which allows very efficient computation for very large _k_ values, e.g., 10,000, on networks with hundreds of thousands of edges. PathLinker outputs the sub-network composed of the _k_ shortest paths.

PathLinker supports several different options for weighting the edges in the network. If the edges in the input graph do not have weights, PathLinker computes the _k_ lowest cost paths, where the cost of a path is the number of edges in it. If the input graph has edge weights, they can be treated additively or as probabilities. If the edge weights are additive, PathLinker defines the cost of a path as the sum of the edge weights and computes the _k_ paths of lowest cost. In the case of probabilities, PathLinker treats the edge weights as multiplicative and computes the _k_ highest cost paths, where the cost of a path is the product of the edge weights.

See the publications referenced below for a formal description of the method, comprehensive evaluations and comparisons to other approaches, and experimental validation of results.

## How to use PathLinker

### Running the plugin

To run the plugin, fill in the inputs (as described below) and press the submit button. Note that PathLinker runs on the currently selected network.

### Inputs

**Sources/Targets:** The user can select nodes directly from the network and then use the buttons 'Add selected sources' and 'Add selected targets' to add the selected nodes names to the corresponding text field. Node names can also be entered manually, separated by spaces. If there are sources or targets named that do not exist in the network, PathLinker will warn the user. 

There are two options available options here:

*   _Allow sources and targets in paths_: Normally, PathLinker removes incoming edges to sources and outgoing edges from targets before computing paths. If the user selects this option, PathLinker will not remove these edges. Therefore, source and target nodes can appear as intermediate nodes in paths computed by PathLinker.

*   _Targets are identical to sources_: If the user selects this option, PathLinker will copy the sources to the targets field. This option allows the user to compute a sub-network of paths connecting any of the sources/targets to each other. If the user selects this option, then PathLinker will automatically allow sources and targets to appear in paths, i.e., the previous option is also selected. Note that since PathLinker computes loopless paths, if the user inputs only a single node and selects this option, PathLinker will not compute any paths at all.

**Algorithm:** There are three parameters here:

* _k_: PathLinker takes in the number of paths the user wants. If an invalid value is input for _k_ (e.g., a negative number or a non-integer), PathLinker will compute the default _k_ = 200 paths.
* _Edge penalty_: PathLinker can apply an edge penalty when using the additive or multiplicative edge weight options. The larger the value of edge penalty, the less likely it is that longer paths will appear in the results before low-weight shorter paths.
  * _Weights are additive_: penalize each path (i.e. add to the total path score) by a factor of (number of edges in the path)*(edge penalty). Default is 0. Numbers > 0 are allowed.
  * _Weights are probabilities_: penalize each path (i.e. add to the total path score) by a factor of (number of edges in the path)^(edge penalty). Default is 1. Numbers >= 1 are allowed.
* _Include tied paths_: If this option is selected, PathLinker will output the *k* lowest cost paths, as well as all paths "tied" (i.e., equal path cost) with the _k<sup>th</sup>_ path.


**Edge weights:** There are three options for the edge weights:

*   _Unweighted_: PathLinker will compute the _k_ lowest cost paths, where the cost is the number of edges in the path
*   _Weights are additive_: PathLinker will compute the _k_ lowest cost paths, where the cost is the sum of the edge weights
*   _Weights are probabilities_: PathLinker will compute the _k_ highest cost paths, where the cost is the product of the edge weights

When a weighted option is selected, the user must select which column to use as the edge weight using `Edge weight column` drop-down. All columns in the `Edge Table` that are of the type `Double`, `Integer`, `Float`, and `Long` can be selected.

### Output

PathLinker generates three outputs:

1. A **`PathLinker-results-X-k-paths`** sub-network consisting of the nodes and edges involved in the _k_ paths.
2. A **`PathLinker Results X`** results panel that displays a table of the rank, score, and nodes of the _k_ paths.
  * Selecting one or more paths in results table will select or highlight those paths in the sub-network.
  * The results can be exported to a TSV (tab separated file).
  * All three of these outputs can be deleted using the `Discard` button. 
3. A **`Path Rank X`** column in the `Edge Table` whose value is the rank of the first path in which a given edge appears.

## How to Cite PathLinker

We will be very glad to hear from you if you use PathLinker in your work. If you publish a paper that uses PathLinker, please cite:

1. [The PathLinker app: Connect the dots in protein interaction networks](https://f1000research.com/articles/6-58/v1). Daniel Gil, Jeffrey Law, Li Huang and T. M. Murali. F1000Research 2017, 6:58 
2. [Pathways on Demand: Automated Reconstruction of Human Signaling Networks](http://www.nature.com/articles/npjsba20162). Anna Ritz, Christopher L. Poirel, Allison N. Tegge, Nicholas Sharp, Allison Powell, Kelsey Simmons, Shiv D. Kale, and T. M. Murali, Systems Biology and Applications, a Nature partner journal, 2, Article number 16002, 2016. 

## Contact Information

If you have any problems using PathLinker or any suggestions for improvement, please contact us by email at jeffl@vt.edu or murali@cs.vt.edu
