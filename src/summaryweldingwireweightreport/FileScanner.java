
package summaryweldingwireweightreport;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

public class FileScanner {
    private final static Map<StanDiameter, String> s_StanDiametersFolders;
    
    static {
        final HashMap<StanDiameter, String> diametersFolders = new HashMap<>();
        
        final StanDiameter[] allDiameters = {
            StanDiameter.STAN_800,
            StanDiameter.STAN_1000
        };
        for(StanDiameter currDiameter: allDiameters) {
            diametersFolders.put(currDiameter, 
                            Integer.toString(currDiameter.getDiameter()));
        }
        
        s_StanDiametersFolders = diametersFolders;
    }
    
    
    private final static Map<StanType, String> s_StanTypeFolders;
    
    static {
        final HashMap<StanType, String> typeFolders = new HashMap<>();
        
        final StanType[] allTypes = {
            StanType.ID,
            StanType.OD
        };
        for(StanType currType: allTypes) {
            typeFolders.put(currType, 
                            currType.getType());
        }
        
        s_StanTypeFolders = typeFolders;
    }
    
    private final String m_rootPath;
    
    
    public FileScanner(String rootPath) {
        m_rootPath = rootPath;
    }
    
    
    public List<Stan> scan(LocalDateTime dt) {
        final ArrayList<Stan> stanStatistics = new ArrayList<>();
        
        final String yearFolderName = Integer.toString(dt.getYear());
        final String monthFolderName = Integer.toString(dt.getMonthValue());
        
        s_StanDiametersFolders.forEach((stanDiameter, folderName) -> {
            try {
                Path stanDiameterPath = Paths.get(m_rootPath, folderName);
                
                List<String> stanTypeFolders = Files.list(stanDiameterPath)
                        .filter(Files::isDirectory)
                        .filter(path -> {
                            final String fileName = path.getFileName().toString();
                            return s_StanTypeFolders
                                    .values()
                                    .stream()
                                    .anyMatch(prefix -> fileName.startsWith(prefix));
                        })
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
                
                stanTypeFolders.forEach(stanTypeFolder -> {
                    Path filePath = Paths.get(
                        stanDiameterPath.toString(),
                        stanTypeFolder,
                        yearFolderName,
                        monthFolderName
                    );
                    List<Path> fileList = null;
                    try {                        
                        fileList = Files.list(filePath)
                                .filter(Files::isRegularFile)
                                .filter(path -> path.toString().toLowerCase().endsWith(".xls"))
                                .collect(Collectors.toList());
                    }
                    catch(Exception ex) {     
                    }
                    if (fileList == null) {
                        return;
                    }
                    
                    final Stan.Builder stanBuilder = new Stan.Builder();
                    stanBuilder.stanDiameter = stanDiameter;
                    stanBuilder.wireWeight = getWireWeight(fileList);
                    stanBuilder.stanType = Arrays.asList(StanType.values())
                        .stream()
                        .filter(stanType -> {
                            final String prefix = stanType.getType();
                            return (prefix != null && prefix.length() > 0)
                                    ? stanTypeFolder.startsWith(stanType.getType())
                                    : false;
                         })
                        .findFirst()
                        .get();
                    stanBuilder.stanNumber = parseStanNumber(stanTypeFolder);
                    final Stan stan = stanBuilder.build();
                    if (stan != null) {
                        stanStatistics.add(stan);
                    }
                });
            }
            catch(Exception ex) {     
            }            
        });
        
        return stanStatistics;
    }
    
    
    private double getWireWeight(List<Path> fileList) {        
        class Wire {
            double weight;
            int count;
        };
        final Wire wire = new Wire();  
        
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final ArrayList<Future<Double>> results = new ArrayList<>();
        fileList.forEach(filePath -> {
            Callable<Double> task = () -> {
                final Double wireWeight = getWireWeight(filePath);
                //println(filePath.toString());
                return wireWeight;
            };
            final Future<Double> oneResult = executor.submit(task);
            results.add(oneResult);
        });
        
        results.forEach(currResult -> {
            try {
                wire.weight += currResult.get();
                wire.count++;
            }
            catch(Exception ex) {                
            }
        });
        
        executor.shutdown();
        
        return wire.weight;
    }
    
    
    private double getWireWeight(Path filePath) {
        double wireWeight = 0.0;
        try (FileInputStream inputStream = new FileInputStream(filePath.toFile())) {
            try (final HSSFWorkbook workbook = new HSSFWorkbook(inputStream)) {
                final HSSFSheet sheet = workbook.getSheet("Setup");
                wireWeight = getWireWeight(sheet);
            }
        }
        catch(Exception ex) {            
        }
        return wireWeight;
    }
    
    
    private double getWireWeight(HSSFSheet sheet) {
        return getCellValue(sheet, 38, "C");
    }

    
    private double getCellValue(HSSFSheet sheet, int row, String col) {
        final String cell = col +
                            Integer.toString(row + 1);
        final CellReference cr = new CellReference(cell);
        final Row rowObj = sheet.getRow(cr.getRow());
        if (rowObj == null) {
            return 0;
        }
        final Cell cellObj = rowObj.getCell(cr.getCol());
        if (cellObj == null ||
            cellObj.getCellTypeEnum() == CellType.BLANK) {
            return 0;
        }
        
        return cellObj.getNumericCellValue();
    }
    
    
    private Integer parseStanNumber(String txt) {
        Integer value = null;
        try {
            value = Integer.parseInt(txt.replaceAll("[A-Za-z]", ""));
        }
        catch(Exception ex) {            
        }
        return value;
    }
    
    
    private synchronized void println(String text) {
        System.out.println(text);
    }
}
