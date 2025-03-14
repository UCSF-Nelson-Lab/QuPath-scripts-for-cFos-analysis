import qupath.lib.gui.QuPathGUI
import qupath.lib.projects.Project
import qupath.lib.images.servers.ImageServer
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.roi.interfaces.ROI
import qupath.lib.regions.RegionRequest
import java.awt.geom.Area


Project project = QuPathGUI.getInstance().getProject()

String outputPath = 'C:\\Users\\Nelson Lab\\Desktop\\cfos_analysis\\INSERT_MOUSE_FOLDER_HERE\\Cfos_counts\\output_area_INSERT_MOUSE_NAME_HERE.csv'

new File(outputPath).withPrintWriter { out ->
    out.println("Image, Countour, Area (sq microns scaled down by 10^4)")
    
    project.getImageList().each { entry ->
        //open image
        def imageData = entry.readImageData()
        def server = imageData.getServer()
        //QPEx.setImageServer(server)
        
        def pixelWidthMicrons = 0.7335
        def areaConversionFactor = (pixelWidthMicrons * pixelWidthMicrons)/10000
        
        imageData.getHierarchy().getAnnotationObjects().forEach { annotation ->
            if(annotation.isAnnotation() && annotation.getROI() != null && annotation.getName() != null) {
                double areaPixels = annotation.getROI().getArea()
                double areaSqCm = areaPixels * areaConversionFactor
                String imageName = server.getMetadata().getName()
                String contourName = annotation.getName()
                
                out.println("${imageName},${contourName},${areaSqCm}")
            }
        }
        
    }
    
    
    
}