package assignment;

import java.util.*;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.instance.model.api.IBaseBundle;

/**
 * This class contains methods for reading resources from the FHIR server.
 */
public class SimpleRead {

    IGenericClient client = null;

    public SimpleRead(String baseUrl) {
        FhirContext ctx = FhirContext.forDstu3();
        baseUrl = "http://hapi.fhir.org/baseDstu3";
        client = ctx.newRestfulGenericClient(baseUrl);
    }

    /**
     * Find the patient with the given ID and return the full name as a
     * single string.
     */
    public String getNameByPatientID(String id) {
        // Hint, there is a method that will return the full name including
        // prefix, first, last, and suffix
        //Place your code here
        FhirContext ctx = FhirContext.forDstu3();
        String baseUrl = "http://hapi.fhir.org/baseDstu3";
        client = ctx.newRestfulGenericClient(baseUrl);
        Patient patient = client.read()
                .resource(Patient.class)
                .withId(id)
                .execute();

        HumanName name = patient.getName().get(0);

        String outpatientName = name.getNameAsSingleString();

        return outpatientName;
    }

    /**
     * Find all the patients that have the provided name and return a list
     * of the IDs for those patients.  The search should include matches
     * where any part of the patient name (family, given, prefix, etc.)
     * matches the method 'name' parameter.
     */
    public List<String> getIDByPatientName(String name) {
        //Place your code here
        FhirContext ctx = FhirContext.forDstu3();
        String baseUrl = "http://hapi.fhir.org/baseDstu3";
        client = ctx.newRestfulGenericClient(baseUrl);
        Bundle bundle = client.search()
                .forResource(Patient.class)
                .where(Patient.NAME.matchesExactly().value(name))
                .returnBundle(Bundle.class)
                .execute();

        //Set<String> retStr = new HashSet<String>();
        Set<String> retStr = fetchRestOfBundle(client, bundle);
        List<String> retVal = new ArrayList<String>(retStr);

        System.out.println("count of entires in the bundle : "+ retVal.size());
        return retVal;
    }


/*

Method to loop thru the bundle results and return the results as a Set (collection with no duplicate values)

 */

    public static Set<String> fetchRestOfBundle(IGenericClient theClient, Bundle theBundle) {
        Set<String> resourcesAlreadyAdded = new HashSet<String>();
        addInitialUrlsToSet(theBundle, resourcesAlreadyAdded);
        Bundle partialBundle = theBundle;
        for (;;) {
            if (partialBundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                partialBundle = theClient.loadPage().next(partialBundle).execute();
                addAnyResourcesNotAlreadyPresentToBundle(theBundle, partialBundle, resourcesAlreadyAdded);
            } else {
                break;
            }
        }
        theBundle.getLink().clear();
        return resourcesAlreadyAdded;
    }

    /*
    Method to add the URLs to Set
     */
    private static void addInitialUrlsToSet(Bundle theBundle, Set<String> theResourcesAlreadyAdded) {
        for (Bundle.BundleEntryComponent entry : theBundle.getEntry()) {
            theResourcesAlreadyAdded.add(entry.getResource().getIdElement().toUnqualifiedVersionless().getIdPart());
        }
    }

    /*
    Add missing resources to the Set if not present/already added

     */

    private static void addAnyResourcesNotAlreadyPresentToBundle(Bundle theAggregatedBundle, Bundle thePartialBundle, Set<String> theResourcesAlreadyAdded) {
        for (Bundle.BundleEntryComponent entry : thePartialBundle.getEntry()) {
            if (!theResourcesAlreadyAdded.contains(entry.getFullUrl())) {
                theResourcesAlreadyAdded.add(entry.getResource().getIdElement().toUnqualifiedVersionless().getIdPart());
                theAggregatedBundle.getEntry().add(entry);
            }
        }
    }

}