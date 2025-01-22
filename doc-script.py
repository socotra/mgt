import os
import csv
import json
import requests

def main():
    try:
        print(os.getcwd())
        with open('../mgt/MVPDocs.csv') as mvp_csv:

            mpvCSVRows = csv.DictReader(mvp_csv)

            json_string =  '{ "scope" : "policy", "format" : "pdf", "rendering" : "prerendered", "trigger" : "priced", "pageSize": "letter", "portrait" : true}'

            python_dict = json.loads(json_string)

            print(python_dict)

            for row in mpvCSVRows:
                save_path = f'allDocuments/{row["formsGL"]}/config.json'

                with open(save_path, 'w') as outfile:
                    json.dump(python_dict, outfile)
                    

    except Exception as error:
        print("An error occurred while trying to print the row, ", error)


if __name__ == "__main__":
    main()
