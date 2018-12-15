import { Component, OnInit } from '@angular/core';
import { SmartServiceJSONService } from './smartServiceJSON.service';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

@Component({
    selector: 'winery-smart-service-json',
    templateUrl: './smartServiceJSON.component.html',
    providers: [SmartServiceJSONService],
    styleUrls: [
        './css/smartServiceJSON.css'
    ]
})

export class SmartServiceJSONComponent {

    //jsonSchema - "This document provides a schema for smart services which supports the specification of their data
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

    //Viewname of selected Item
    exampleData = [
        { id: 0, name: "Example data input" },
        { id: 1, name: "Data Providers" },
        { id: 2, name: "Data Processor" },
        { id: 3, name: "Data Consumers" }
    ];

    //Create UI dependent of Templates
    public selectTemplate(event: any): void {
        const newSelectedVal = event.target.value;
        if (newSelectedVal == 0) {  //if is nothing selected
            this.jsonFormLayout = [{
                "type": "submit",
                "title": "Save",
                "disabled": true
            }];
            this.flexibleData = {};
        }
        if (newSelectedVal == 1) { //if Data Provider is seleted
            this.jsonFormLayout = this.providerLayout,
                this.flexibleData = require("./model/smart-service-data-provider.json"),
                this.serviceClass = 1;

        }
        if (newSelectedVal == 2) { //if Data Processor is selected
            this.jsonFormLayout = this.processorLayout,
                this.flexibleData = require("./model/smart-service-data-processor.json")
            this.serviceClass = 2;
        }
        if (newSelectedVal == 3) { //if Data Consumer is selected

            this.jsonFormLayout = this.consumerLayout,
                this.flexibleData = require("./model/smart-service-data-consumer.json")
            this.serviceClass = 3;

        }
        console.log(newSelectedVal);
    }


    file: File = null;
    /**
     * read and load data of extern file
     * @param {FileList} files
     */
    chooseFile(files: FileList) {
        this.file = files.item(0);

        let fileReader = new FileReader();
        fileReader.onload = (e) => {
            let data = JSON.parse(fileReader.result); //data input (string object) parse into json
            if((data.SmartService.DataProviders)!=null) {
                this.jsonFormLayout = this.providerLayout,
                    this.flexibleData = data
            }
            if((data.SmartService.DataProcessors)!=null) {
                this.jsonFormLayout = this.processorLayout,
                    this.flexibleData = data
            }
            if((data.SmartService.DataConsumers)!=null)  {
                this.jsonFormLayout = this.consumerLayout,
                    this.flexibleData = data
            }
        }

        fileReader.readAsText(this.file);

    }

    //download data
    public save(event: any): void {
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
