package org.eclipse.winery.model.converter;

import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class ConverterTest {
    
    
    @Test
    public void testConvertX2Y() throws Exception {

        ClassLoader classLoader = new ConverterTest().getClass().getClassLoader();
        InputStream input = classLoader.getResourceAsStream("test.xml");
        Converter converter = new Converter();
        
        InputStream output = converter.convertX2Y(input);
        
        StringWriter outputStringWriter = new StringWriter();
        IOUtils.copy(output, outputStringWriter, "UTF-8");
        
        InputStream expectedInputStream = classLoader.getResourceAsStream("test.yaml");
        StringWriter expectedStringWriter = new StringWriter();
        IOUtils.copy(expectedInputStream,expectedStringWriter, "UTF-8");
        
        Assert.assertEquals(outputStringWriter.toString(), expectedStringWriter.toString());
        
    }
    
    
}
