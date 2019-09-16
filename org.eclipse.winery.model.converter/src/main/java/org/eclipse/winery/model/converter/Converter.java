package org.eclipse.winery.model.converter;

import java.io.InputStream;

import org.eclipse.winery.model.converter.x2y.X2YConverter;

public class Converter {
    
    public Converter(){
        
    }
    
    public InputStream convertX2Y(InputStream inputStream) {
        X2YConverter converter = new X2YConverter();
        return converter.covertX2Y(inputStream);
    }
    
}
