import qupath.ext.stardist.StarDist2D
import qupath.lib.io.PathIO
import java.io.File
import qupath.lib.algorithms.ObjectClassifierTools.*;
import qupath.opencv.ops.ImageOps
import static qupath.lib.gui.scripting.QPEx.*
import groovy.time.*
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.PathObject
import qupath.lib.roi.interfaces.ROI
import qupath.lib.measurements.MeasurementList
import qupath.lib.objects.PathDetectionObject
import ij.IJ
import qupath.imagej.tools.IJTools
import qupath.lib.objects.PathObjects
import qupath.lib.regions.RegionRequest
import qupath.lib.roi.PolygonROI
import ij.process.ImageProcessor
import ij.plugin.frame.RoiManager
import qupath.lib.images.servers.ImageServer
import qupath.lib.objects.classes.PathClassFactory
import qupath.lib.images.ImageData
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.servers.PixelCalibration
import ij.process.ShortProcessor
import java.awt.image.DataBufferUShort
import qupath.lib.gui.QuPathGUI
import java.awt.Color;
import qupath.imagej.tools.PathImagePlus
import ij.process.ByteProcessor;
import java.awt.image.BufferedImage
import qupath.lib.images.PathImage



//import qupath.tensorflow.stardist.StarDist2D

// Specify the model file 
var pathModel = 'C:/Users/Nelson Lab/Downloads/QuPath-0.4.3-Windows/he_heavy_augment/dsb2018_heavy_augment.pb'

var imageData = getCurrentImageData()
var pathObjects = getSelectedObjects()

cell_area_measurement = 'AF594: Area µm^2'
cell_intensity_measurement = 'AF594: Mean'

def server = imageData.getServer()

var resolution = server.getPixelCalibration()



var stardist = StarDist2D.builder(pathModel)
        .threshold(0.67)              // Probability (detection) threshold
        .preprocess(// Extra preprocessing steps, applied sequentially
           ImageOps.Channels.extract(0),
           ImageOps.Filters.median(1),
           ImageOps.Core.divide(1),
           ImageOps.Core.add(0)
         )
        
        .includeProbability(true)
        .measureIntensity()
        .channels('FITC')            // Specify detection channel
        .normalizePercentiles(0.2, 99.8) // Percentile normalization
        .pixelSize(1.0) //1.754
        .tileSize(1048)  // Resolution for detection
        .cellExpansion(1)
        .cellConstrainScale(1)
      //  .measureShape()
        .constrainToParent(false)
        .nThreads(4)
        .simplify(1)
        .ignoreCellOverlaps(false)
        .build()
 
 
 
 /*
 var stardist = StarDist2D.builder(pathModel)
                // Probability (detection) threshold
        .preprocess(// Extra preprocessing steps, applied sequentially
           StarDist2D.imageNormalizationBuilder()
           .maxDimension(4096) 
           .percentiles(0.2,99.8)
           .build()
         )
        .pixelSize(1.0)
        .threshold(0.75)
 */

//some considerations for parameter values:
// we can have threshold set to 0.734
// normalize Percentile values is set to 5 and 99
//pixel size is set to 1.6965
//median is set to 2

//with one of the sections (section/scene 10), it is good about not getting false positives with the 
//value above. It is not detecting the obvious objects though. Scenes 1 and 2 are consistent with scene 3

//Scene 5 has more false positives compared to other sections


// Run detection for the selected objects




if (pathObjects.isEmpty()) {
    Dialogs.showErrorMessage("StarDist", "Please select a parent object!")
    return
}

stardist.detectObjects(imageData, pathObjects)

int channelIndex = 0
 
 


//def objectsOfInterest = getAllObjects().findAll { it.getPathClass() == cFosClass }

//def selectedAnnotation = getSelectedObjects().findAll{ it instanceof PathAnnotationObject}

/*
if(selectedAnnotation.isEmpty()) {
    print("No annotation is selected.")
    return
}
*/

//def targetAnnotation = selectedAnnotation.first()

def selectedObj = getSelectedObjects();



    def cFosCells = getDetectionObjects().findAll{ object ->
    def roi = object.getROI()
    def area = roi.getArea()
    
     // println "the area of this detection object is: " + area
    
    if((area >= 700)) {
       removeObject(object, true)
    }
    
        return true
    }
    



    






//to debug

var counter = 0;

def cFosClass = getPathClass('cFos')




cFosCells.each {
   // counter = counter +1; //debug
   // it.setPathClass(testClass)
   
   it.setPathClass(cFosClass)
   
   //println "Area: ${it.getMeasurementList().getMeasurementValue('Area um^2')}"
   
   
   if(it.getPathClass().equals(cFosClass)) {
       counter = counter +1;
   }
   
   
   
   
}



Platform.runLater({
    fireHierarchyUpdate()
})

def cFosClassObjects = getAllObjects().findAll(pathObject -> pathObject.getPathClass() == getPathClass('cFos'))

//println "the value of cfos size is: " + cFosClassObjects.size();


/*
def outputFile = new File('C:\\Users\\Nelson Lab\\Desktop\\cfos_analysis\\ss230906a\\Cfos_counts\\output_ss230906a.csv')

//C:\Users\Nelson Lab\Desktop\cfos_analysis\ss230906a\Cfos_counts


if(!(outputFile.exists())) {
    outputFile.withWriter { writer ->
        writer.writeLine("Image Name, c-fos Script Quantification")
    }
}


 def selectedObjects = getSelectedObjects()
 def pthObjects = []
    
selectedObjects.each { PathObject pathObject ->

        int count = 0;
        
        //count all detections within this annotation
        def children = pathObject.getChildObjects().findAll {
            it instanceof PathDetectionObject
            count++;
        }
        
    //    println "the value of count is : " + count;
        
         
        def imageName = server.getMetadata().getName()
        
       outputFile.withWriterAppend { writer ->
           writer.writeLine("$imageName,$count")
       }
 
 }
 */
 
      
    
    
//-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//Colocalization Quantification
    
 int d1TmtChannel = 2
 double intensityThreshold = 1;
 def hierarchy = imageData.getHierarchy()
 String objectType = "cFOScells"
 int cFosChannel = 1
 int ch1Background = 1000
 int ch2Background = 10000
 double pearsonThreshold = 0.5
 def colocarr = [];
 int loc;
 

String path = server.getPath()


ImageServer<BufferedImage> serverOg = server


def selectO = getSelectedObjects()


selectO.each { PathObject pObj ->
    if(pObj instanceof PathAnnotationObject) {
        def child = pObj.getChildObjects().findAll {
            it instanceof PathDetectionObject
            //count++;
            
            
            
             //extract roi for each cfos positive cell
    ROI roi = it.getROI()
    RegionRequest request = RegionRequest.createInstance(imageData.getServerPath(), 1.0, it.getROI())
    
    pathImage = IJTools.convertToImagePlus(server, request)
    imp = pathImage.getImage()
    
    cFosChanImage = imp.getStack().getProcessor(cFosChannel)
    cFosChanImage = cFosChanImage.convertToFloatProcessor() //Needed to handle big numbers
    cFosChPixels = cFosChanImage.getPixels()
    
    bpSLICs = createObjectMask(pathImage, it).getPixels()
    
    size = cFosChPixels.size()
    
    TmtChanImage = imp.getStack().getProcessor(d1TmtChannel)
    TmtChanImage = TmtChanImage.convertToFloatProcessor()
    
    TmtChPixels = TmtChanImage.getPixels()
   

    ch1cFos = [];
    ch2Tmt = [];
    
    for (i=0; i<size; i++){
         if(bpSLICs[i]) {
             ch1cFos<<cFosChPixels[i] 
             ch2Tmt<<TmtChPixels[i]

         }
    }
    
     if(ch1cFos.size() == 0 || ch2Tmt.size() == 0) {
         return
     }
     
     
        
       //Calculating the mean for Pearson's
        double ch1Mean = ch1cFos.sum()/ch1cFos.size()
        double ch2Mean = ch2Tmt.sum()/ch2Tmt.size()
         //get the new number of pixels to be analyzed
         size = ch1cFos.size()
         
      //calculating pearson's colocalization coefficient
      top = []
         for (i=0; i<size;i++){top << (ch1cFos[i]-ch1Mean)*(ch2Tmt[i]-ch2Mean)}
         pearsonTop = top.sum()
 
         //Sums for the two bottom parts
         botCh1 = []
         for (i=0; i<size;i++){botCh1<< (ch1cFos[i]-ch1Mean)*(ch1cFos[i]-ch1Mean)}
         rootCh1 = Math.sqrt(botCh1.sum())
 
         botCh2 = []
         for (i=0; i<size;i++){botCh2 << (ch2Tmt[i]-ch2Mean)*(ch2Tmt[i]-ch2Mean)}
         rootCh2 = Math.sqrt(botCh2.sum())
         
         pearsonBot = rootCh2*rootCh1
         
         double pearson = pearsonTop/pearsonBot
         
       //  println "pearson value is: " + pearson
         
         String name = "Pearson Corr "+objectType+":"+cFosChannel+"+"+d1TmtChannel
         it.getMeasurementList().putMeasurement(name, pearson)
         
         
         
         
         if(pearson >= pearsonThreshold) {
             it.setColor(10);
             colocarr.add(1);
             loc++;
         }
         
         
        //Start Manders calculations
         double m1Top = 0
         for (i=0; i<size;i++){if (ch2Tmt[i] > ch2Background){m1Top += Math.max(ch1cFos[i]-ch1Background,0)}}
         
         double m1Bottom = 0
         for (i=0; i<size;i++){m1Bottom += Math.max(ch1cFos[i]-ch1Background,0)}
         
         double m2Top = 0
         for (i=0; i<size;i++){if (ch1cFos[i] > ch1Background){m2Top += Math.max(ch2Tmt[i]-ch2Background,0)}}
         
         double m2Bottom = 0
         for (i=0; i<size;i++){m2Bottom += Math.max(ch2Tmt[i]-ch2Background,0)}
         
         //Check for divide by zero and add measurements
          name = "M1 "+objectType+": ratio of Ch"+cFosChannel+" intensity in Ch"+d1TmtChannel+" areas"
          double M1 = m1Top/m1Bottom
          
          if (M1.isNaN()){M1 = 0}
            it.getMeasurementList().putMeasurement(name, M1)
            double M2 = m2Top/m2Bottom
           if (M2.isNaN()){M2 = 0}
           name = "M2 "+objectType+": ratio of Ch"+d1TmtChannel+" intensity in Ch"+cFosChannel+" areas"
            it.getMeasurementList().putMeasurement(name, M2)
            
      //    println("Manders M1 and M2 for detection: " + M1 + "and" + M2) //debug
        }
    }
    
}
 
   def outputFile = new File('C:\\Users\\Nelson Lab\\Desktop\\cfos_analysis\\rp230821e\\Cfos_counts\\output_rp230821e.csv')

//C:\Users\Nelson Lab\Desktop\cfos_analysis\ss230906a\Cfos_counts


     if(!(outputFile.exists())) {
        outputFile.withWriter { writer ->
           writer.writeLine("Image Name, Annotation Name, c-fos Script Quantification, Colocalization Quantification")
        }
    }


     def selectedObjects = getSelectedObjects()
     def pthObjects = []
    
    selectedObjects.each { PathObject pathObject ->

        int count = 0;
        //loc = 0;
       
        def annotationName = pathObject.getName()
        
        
        //count all detections within this annotation
        def children = pathObject.getChildObjects().findAll {
            it instanceof PathDetectionObject
            count++;
        }
        
        println "the value of count is : " + count;
        
         
        def imageName = server.getMetadata().getName()
        
       outputFile.withWriterAppend { writer ->
           writer.writeLine("$imageName,$annotationName,$count, $loc")
       }
       
 
     }
     
     colocarr.clear();
     arrsize = colocarr.size()
     println "the size of loc is: " + loc;
   
    
    
    def createObjectMask(PathImage pathImage, PathObject object) {
         //create a byteprocessor that is the same size as the region we are analyzing
         def bp = new ByteProcessor(pathImage.getImage().getWidth(), pathImage.getImage().getHeight())
         //create a value to fill into the "good" area
         bp.setValue(1.0)

         
         def region = object.getROI()
         roiIJ = IJTools.convertToIJRoi(region, pathImage)
         bp.fill(roiIJ)
 
         return bp
     }
    
    
    
 //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------scratch work   
    
    //get the image processor for D1 Tmt channel within the ROI
 //   def img = server.readBufferedImage(request)
  //  def raster = img.getRaster()
 //   def dataBuffer = raster.getDataBuffer()
    
    /*
    short[] pixels;
    
    if(dataBuffer instanceof DataBufferUShort) {
        pixels = ((DataBufferUShort) dataBuffer).getData()
    } else {
        throw new IllegalArgumentException("Unsupported data buffer type: " + dataBuffer.getClass().getSimpleName())
    }
    
    def sp = new ShortProcessor(raster.getWidth(), raster.getHeight(), pixels, null)
    
    double meanIntensity = sp.getStatistics().mean
    
    //int c = 0;
    
    if(meanIntensity >= intensityThreshold) {
        cFosObject.setPathClass(PathClassFactory.getPathClass("Colocalized c-Fos with D1 TMT"))
       // cFosObject.setColor(13);
    }
    */
    
    
    
    
    
    
    
    /*
    def viewer = QuPathGUI.getInstance().getViewer();
    def viewerImageData = viewer.getImageData();
    
    def cobjects = imageData.getHierarchy().getRootObject().getChildObjects();
    
    def colocalizedClass = PathClassFactory.getPathClass("Colocalized c-Fos with D1 TMT");
    def nonColocalizedClass = PathClassFactory.getPathClass("c-Fos")
    
    int c = 0;
    
    println "the value of nonlocalized class is: " + nonColocalizedClass.size()
    
    cobjects.each { PathObject object ->
        //if(object.isDetection() && object.getPathClass() == colocalizedClass) {
          if(object.isDetection())
            object.setColor(13);
            c = c+1;
            
  //     } //else if(object.isDetection() && object.getPathClass() == noncolocalizedClass) 
            //object.setColor(6);
        }
        
        println "the value of c is: " + c
        
    
    
    
  //  def pixels = raster.getPixels(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight(), new float[raster.getWidth() * raster.getHeight()])
  */
   


fireHierarchyUpdate()
println 'Colocalization assessment completed.'


