import pandas as pd
from py2cytoscape.data.cyrest_client import CyRestClient


# create an instance of cyRest client
cy = CyRestClient()

# create a new network from the input txt file
test_network_input = pd.read_csv('src/test/resources/input/test.txt', header=None, delimiter=r"\s+")

source = test_network_input.columns[0]
target = test_network_input.columns[1]
edge = test_network_input.columns[2]
interaction = test_network_input.columns[3]
title = "test run"

test_network = cy.network.create_from_dataframe(test_network_input, source_col=source, target_col=target, interaction_col=interaction, name=title)
