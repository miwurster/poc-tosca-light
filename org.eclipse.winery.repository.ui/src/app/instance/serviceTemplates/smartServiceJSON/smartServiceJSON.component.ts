import { Component, OnInit } from '@angular/core';
import { SmartServiceJSONService } from './smartServiceJSON.service';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

@Component({
    selector: 'winery-smart-service-json',
    templateUrl: './smartServiceJSON.component.html',
    providers: [SmartServiceJSONService],
    styleUrls: [
        './css/custom.css'
    ]
    //styleUrls: ['./bootstrap.min.css']
})

export class SmartServiceJSONComponent {

    //schema - "This document provides a schema for smart services which supports the specification of their data
    // providers, data consumers and/or data processors.",
    jsonSchema = require("./model/smart-service-schema.json");
    providerLayout = require("./model/smart-service-layout_provider.json"); // layout Providers
    processorLayout = require("./model/smart-service-layout_processor.json"); //layout Processors
    consumerLayout = require("./model/smart-service-layout_consumer.json"); //layout Consumers

    //default layout and data input
    jsonFormLayout = [{
        "type": "submit",
        "title": "Save",
        "disabled": true
    }];
    flexibleData = {};

    serviceClass: any;

    // compound Object for smartServiceJSON.component.html
    compoundObject = {
        schema: this.jsonSchema,
        layout: this.jsonFormLayout,
        data: this.flexibleData
    };

    exampleData = [
        { id: 0, name: "Example data input" },
        { id: 1, name: "Data Providers" },
        { id: 2, name: "Data Processor" },
        { id: 3, name: "Data Consumers" }
    ];

    public onChange(event: any): void {  // event will give you full brief of action
        const newVal = event.target.value;
        if (newVal == 0) {
            this.jsonFormLayout = [{
                "type": "submit",
                "title": "Save",
                "disabled": true
            }];
            this.flexibleData = {};
        }
        if (newVal == 1) {
            this.jsonFormLayout = this.providerLayout,
                this.flexibleData = require("./model/smart-service-data-provider.json"),
                this.serviceClass = 1;

        }
        if (newVal == 2) {
            this.jsonFormLayout = this.processorLayout,
                this.flexibleData = require("./model/smart-service-data-processor.json")
            this.serviceClass = 2;
        }
        if (newVal == 3) {

            this.jsonFormLayout = this.consumerLayout,
                this.flexibleData = require("./model/smart-service-data-consumer.json")
            this.serviceClass = 3;

        }
        console.log(newVal);
    }

    file: File = null;
    i: number;
    /**
     * read and load data of extern file
     * @param {FileList} files
     */
    handleFileInput(files: FileList) {
        this.file = files.item(0);

        let fileReader = new FileReader();
        fileReader.onload = (e) => {
            let data = JSON.parse(fileReader.result); //data input (string object) parse into json
            let serviceClass = data.SmartService.ServiceClass; //get ServiceClass to define layout form and data input
            this.serviceClass = serviceClass;
            if (1 == serviceClass) {
                this.jsonFormLayout = this.providerLayout,
                    this.flexibleData = data
            }
            if (2 == serviceClass) {
                this.jsonFormLayout = this.processorLayout,
                    this.flexibleData = data
            }
            if (3 == serviceClass) {
                this.jsonFormLayout = this.consumerLayout,
                    this.flexibleData = data
            }
            //this.fileUpNotLoad = "Fail to load data! Please select other file or select example Data!"
        }

        fileReader.readAsText(this.file);

    }

    public submit(event: any): void {
        var sJson = JSON.stringify(this.flexibleData);
        var element = document.createElement('a');
        element.setAttribute('href', "data:text/json;charset=UTF-8," + encodeURIComponent(sJson));
        switch (this.serviceClass) {
            case 1:
                element.setAttribute('download', "smart-service-data-provider.json");
                break;
            case 2:
                element.setAttribute('download', "smart-service-data-processor.json");
                break;
            case 3:
                element.setAttribute('download', "smart-service-data-consumer.json");
                break;
            default:
                element.setAttribute('download', "smart-service-data-undifined.json");
        }
        ;
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click(); // simulate click
        document.body.removeChild(element);
    }
}
