package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.Algorithms.Path;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.cytoscape.model.CyNetwork;

/**
 * // -------------------------------------------------------------------------
 * /** Popup that displays the results for the PathLinker CytoScape plugin
 *
 * @author Daniel Gil
 * @version Nov 23, 2015
 */
public class ResultFrame
    extends JFrame
{
    private CyNetwork _network;


    /**
     * Constructor for the result frame class
     *
     * @param network
     *            the associated network the pathlinker was run on
     * @param results
     *            the results from pathlinker
     */
    public ResultFrame(CyNetwork network, ArrayList<Path> results)
    {
        super("PathLinker Results");
        _network = network;

        // creates and writes to the table
        initializeTable(results);
    }


    /**
     * Initializes a JPanel given the results from the plugin
     *
     * @param results
     *            the results from the plugin
     */
    private void initializeTable(ArrayList<Path> results)
    {
        Object[] columnNames =
            new Object[] { "Path index", "Path score", "Path" };
        Object[][] rowData = new Object[results.size()][columnNames.length];

        for (int i = 0; i < results.size(); i++)
        {
            rowData[i][0] = i + 1;
            rowData[i][1] = results.get(i).weight;
            rowData[i][2] = pathAsString(results.get(i));
        }

        // overrides the default table model to make all cells non editable
        class NonEditableModel
            extends DefaultTableModel
        {

            NonEditableModel(Object[][] data, Object[] colNames)
            {
                super(data, colNames);
            }


            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        }

        // populates the table with the path data
        JTable resultTable =
            new JTable(new NonEditableModel(rowData, columnNames));

        // fixes column widths
        TableColumn index = resultTable.getColumnModel().getColumn(0);
        index.setMaxWidth(100);

        TableColumn score = resultTable.getColumnModel().getColumn(1);
        score.setMinWidth(150);
        score.setMaxWidth(200);

        TableColumn path = resultTable.getColumnModel().getColumn(2);
        path.setMinWidth(200);

        // table automatically resizes to fit path column
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // scrollable panel
        JScrollPane scrollPane = new JScrollPane(resultTable);
        this.add(scrollPane);
    }


    /**
     * Converts a path to a string concatenating the node names A path in the
     * network involving A -> B -> C would return A|B|C
     *
     * @param p
     *            the path to convert to a string
     * @return the concatenation of the node names
     */
    private String pathAsString(Path p)
    {
        // builds the path string without supersource/supertarget [1,len-1]
        StringBuilder currPath = new StringBuilder();
        for (int i = 1; i < p.size() - 1; i++)
        {
            currPath.append(
                _network.getRow(p.get(i)).get(CyNetwork.NAME, String.class)
                    + "|");
        }
        currPath.setLength(currPath.length() - 1);

        return currPath.toString();
    }
}
