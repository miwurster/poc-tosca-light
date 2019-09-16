package org.eclipse.winery.repository.backend.filebased;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.winery.model.tosca.TDefinitions;
import org.eclipse.winery.repository.TestWithGitBackedRepository;
import org.eclipse.winery.repository.backend.filebased.converter.X2YConverter;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConverterTest extends TestWithGitBackedRepository {
    
    
    @Test
    public void testConvertX2Y() throws Exception {
        
//        ClassLoader classLoader = new ConverterTest().getClass().getClassLoader();
//        InputStream input = classLoader.getResourceAsStream("converter/test.xml");
//        X2YConverter converter = new X2YConverter(this.repository);
//        
//        
//        InputStream output = converter.convert(input);
//        
//        StringWriter outputStringWriter = new StringWriter();
//        IOUtils.copy(output, outputStringWriter, "UTF-8");
//        
//        InputStream expectedInputStream = classLoader.getResourceAsStream("converter/test.yaml");
//        StringWriter expectedStringWriter = new StringWriter();
//        IOUtils.copy(expectedInputStream,expectedStringWriter, "UTF-8");
//
//        System.out.println(outputStringWriter.toString());
        
    }
    
    
}
