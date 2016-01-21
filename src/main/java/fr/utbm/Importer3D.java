package fr.utbm;

import com.interactivemesh.jfx.importer.ModelImporter;
import com.interactivemesh.jfx.importer.stl.StlMeshImporter;
import com.interactivemesh.jfx.importer.tds.TdsModelImporter;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.io.IOException;

/**
 * Base Importer for all supported 3D file formats.
 */
public final class Importer3D {

    /**
     * Get array of extension filters for supported file formats.
     * 
     * @return array of extension filters for supported file formats
     */
    public static String[] getSupportedFormatExtensionFilters() {
        return new String[] { "*.3ds", "*.stl"};
    }

    /**
     * Load a 3D file, always loaded as TriangleMesh.
     * 
     * @param fileUrl the url of the 3D file to load
     * @return the loaded Node which could be a MeshView or a Group
     * @throws IOException if there is a problem loading the file
     */
    public static Group load(final String fileUrl) throws IOException {

        final int dot = fileUrl.lastIndexOf('.');
        if (dot <= 0) {
            throw new IOException("Unknown 3D file format, url missing extension [" + fileUrl + "]");
        }
        final String extension = fileUrl.substring(dot + 1, fileUrl.length()).toLowerCase();

        switch (extension) {
        case "3ds":
            ModelImporter tdsImporter = new TdsModelImporter();
            tdsImporter.read(fileUrl);
            final Node[] tdsMesh = (Node[]) tdsImporter.getImport();
            tdsImporter.close();
            return new Group(tdsMesh);
        case "stl":
            StlMeshImporter stlImporter = new StlMeshImporter();
            stlImporter.read(fileUrl);
            // STL includes only geometry data
            TriangleMesh cylinderHeadMesh = stlImporter.getImport();

            stlImporter.close();

            // Create Shape3D
            MeshView cylinderHeadMeshView = new MeshView();
            cylinderHeadMeshView.setMaterial(new PhongMaterial(Color.GRAY));
            cylinderHeadMeshView.setMesh(cylinderHeadMesh);

            stlImporter.close();
            return new Group(cylinderHeadMeshView);
        default:
            throw new IOException("Unsupported 3D file format [" + extension + "]");
        }
    }
}
