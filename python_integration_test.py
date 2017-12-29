from py2cytoscape.data.cynetwork import CyNetwork
from py2cytoscape.data.cyrest_client import CyRestClient
import py2cytoscape.util.cytoscapejs as cyjs
import py2cytoscape.cytoscapejs as renderer

import networkx as nx
import pandas as pd
import json

# create an instance of cyRest client
cy = CyRestClient()

# reset the session
cy.session.delete()

# create a new network from the input txt file
test_network_input = pd.read_csv('src/test/resources/input/graph-undir_human-interactome.txt', header=None, sep='\t', lineterminator='\n')

source = test_network_input.columns[0]
target = test_network_input.columns[1]
interaction = test_network_input.columns[2]
title = "test run"

test_network = cy.network.create_from_dataframe(test_network_input, source_col=source, target_col=target, interaction_col=interaction, name=title)
# test_network['interaction'] = test_network['interaction'].astype('float64')
