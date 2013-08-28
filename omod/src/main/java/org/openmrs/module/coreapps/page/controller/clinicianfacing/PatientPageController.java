/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.coreapps.page.controller.clinicianfacing;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.EncounterService;
import org.openmrs.api.VisitService;
import org.openmrs.module.appframework.domain.Extension;
import org.openmrs.module.appframework.service.AppFrameworkService;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.coreapps.contextmodel.VisitContextModel;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.adt.AdtService;
import org.openmrs.module.emrapi.patient.PatientDomainWrapper;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.InjectBeans;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.page.Redirect;
import org.springframework.web.bind.annotation.RequestParam;

import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatientPageController {
	
	public Object controller(@RequestParam("patientId") Patient patient, PageModel model,
	                         @InjectBeans PatientDomainWrapper patientDomainWrapper,
	                         @SpringBean("adtService") AdtService adtService,
	                         @SpringBean("visitService") VisitService visitService,
	                         @SpringBean("encounterService") EncounterService encounterService,
	                         @SpringBean("emrApiProperties") EmrApiProperties emrApiProperties,
                             @SpringBean("appFrameworkService") AppFrameworkService appFrameworkService,
	                         UiSessionContext sessionContext, UiUtils uiUtils) {
		
		if (patient.isVoided() || patient.isPersonVoided()) {
			return new Redirect("coreapps", "patientdashboard/deletedPatient", "patientId=" + patient.getId());
		}
		
		patientDomainWrapper.setPatient(patient);
		model.addAttribute("patient", patientDomainWrapper);
		
		Location visitLocation = null;
		try {
			visitLocation = adtService.getLocationThatSupportsVisits(sessionContext.getSessionLocation());
		}
		catch (IllegalArgumentException ex) {
			// location does not support visits
		}
		
		VisitDomainWrapper activeVisit = null;
		List<Visit> visits = visitService.getVisitsByPatient(patient, true, false);
		List<VisitDomainWrapper> patientVisits = new ArrayList<VisitDomainWrapper>();
		if (visitLocation != null) {
			activeVisit = adtService.getActiveVisit(patient, visitLocation);
		}
		
		//Core API returns visits sorted with latest coming first
		for (Visit v : visits) {
			patientVisits.add(new VisitDomainWrapper(v, emrApiProperties));
			//Limit to 5 visits
			if (patientVisits.size() == 5)
				break;
		}
		
		model.addAttribute("activeVisit", activeVisit);

        SimpleBindings contextModel = new SimpleBindings();
        contextModel.put("patientId", patient.getId());
        contextModel.put("visit", activeVisit == null ? null : new VisitContextModel(activeVisit));

        List<Extension> overallActions = appFrameworkService.getExtensionsForCurrentUser("patientDashboard.overallActions", contextModel);
        Collections.sort(overallActions);
        model.addAttribute("overallActions", overallActions);

        List<Extension> visitActions = appFrameworkService.getExtensionsForCurrentUser("patientDashboard.visitActions", contextModel);
        Collections.sort(visitActions);
        model.addAttribute("visitActions", visitActions);

		model.addAttribute("patientVisits", patientVisits);

		return null;
	}

}
