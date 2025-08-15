/*
 * Copyright 2025 EisenVault
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eisenvault.sitewisepermissions.platformsample;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class CreateUsersWebScript extends DeclarativeWebScript {
    private static Log logger = LogFactory.getLog(CreateUsersWebScript.class);

    private PersonService personService;

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        
                    // Define test users (100+ users with realistic names)
            String[][] users = {
                // CRM Users (35 users)
                {"john.doe", "John", "Doe", "john.doe@company.com"},
                {"jane.smith", "Jane", "Smith", "jane.smith@company.com"},
                {"mike.johnson", "Mike", "Johnson", "mike.johnson@company.com"},
                {"sarah.williams", "Sarah", "Williams", "sarah.williams@company.com"},
                {"david.brown", "David", "Brown", "david.brown@company.com"},
                {"lisa.davis", "Lisa", "Davis", "lisa.davis@company.com"},
                {"james.miller", "James", "Miller", "james.miller@company.com"},
                {"emily.wilson", "Emily", "Wilson", "emily.wilson@company.com"},
                {"robert.moore", "Robert", "Moore", "robert.moore@company.com"},
                {"jennifer.taylor", "Jennifer", "Taylor", "jennifer.taylor@company.com"},
                {"thomas.anderson", "Thomas", "Anderson", "thomas.anderson@company.com"},
                {"jessica.thomas", "Jessica", "Thomas", "jessica.thomas@company.com"},
                {"christopher.jackson", "Christopher", "Jackson", "christopher.jackson@company.com"},
                {"amanda.white", "Amanda", "White", "amanda.white@company.com"},
                {"daniel.harris", "Daniel", "Harris", "daniel.harris@company.com"},
                {"nicole.martin", "Nicole", "Martin", "nicole.martin@company.com"},
                {"matthew.thompson", "Matthew", "Thompson", "matthew.thompson@company.com"},
                {"ashley.garcia", "Ashley", "Garcia", "ashley.garcia@company.com"},
                {"joshua.martinez", "Joshua", "Martinez", "joshua.martinez@company.com"},
                {"stephanie.robinson", "Stephanie", "Robinson", "stephanie.robinson@company.com"},
                {"andrew.clark", "Andrew", "Clark", "andrew.clark@company.com"},
                {"rachel.rodriguez", "Rachel", "Rodriguez", "rachel.rodriguez@company.com"},
                {"kevin.lewis", "Kevin", "Lewis", "kevin.lewis@company.com"},
                {"laura.lee", "Laura", "Lee", "laura.lee@company.com"},
                {"ryan.walker", "Ryan", "Walker", "ryan.walker@company.com"},
                {"michelle.hall", "Michelle", "Hall", "michelle.hall@company.com"},
                {"steven.allen", "Steven", "Allen", "steven.allen@company.com"},
                {"kimberly.young", "Kimberly", "Young", "kimberly.young@company.com"},
                {"timothy.king", "Timothy", "King", "timothy.king@company.com"},
                {"angela.wright", "Angela", "Wright", "angela.wright@company.com"},
                {"jeffrey.lopez", "Jeffrey", "Lopez", "jeffrey.lopez@company.com"},
                {"melissa.hill", "Melissa", "Hill", "melissa.hill@company.com"},
                {"scott.scott", "Scott", "Scott", "scott.scott@company.com"},
                {"rebecca.green", "Rebecca", "Green", "rebecca.green@company.com"},
                {"eric.adams", "Eric", "Adams", "eric.adams@company.com"},
                {"heather.baker", "Heather", "Baker", "heather.baker@company.com"},
                {"gregory.gonzalez", "Gregory", "Gonzalez", "gregory.gonzalez@company.com"},
                {"cynthia.nelson", "Cynthia", "Nelson", "cynthia.nelson@company.com"},
                {"patrick.carter", "Patrick", "Carter", "patrick.carter@company.com"},
                {"deborah.mitchell", "Deborah", "Mitchell", "deborah.mitchell@company.com"},
                
                // HR Users (35 users)
                {"alice.jones", "Alice", "Jones", "alice.jones@company.com"},
                {"bob.wilson", "Bob", "Wilson", "bob.wilson@company.com"},
                {"carol.martinez", "Carol", "Martinez", "carol.martinez@company.com"},
                {"david.anderson", "David", "Anderson", "david.anderson@company.com"},
                {"elizabeth.taylor", "Elizabeth", "Taylor", "elizabeth.taylor@company.com"},
                {"frank.thomas", "Frank", "Thomas", "frank.thomas@company.com"},
                {"grace.hernandez", "Grace", "Hernandez", "grace.hernandez@company.com"},
                {"henry.moore", "Henry", "Moore", "henry.moore@company.com"},
                {"irene.martin", "Irene", "Martin", "irene.martin@company.com"},
                {"jack.lee", "Jack", "Lee", "jack.lee@company.com"},
                {"katherine.perez", "Katherine", "Perez", "katherine.perez@company.com"},
                {"larry.thompson", "Larry", "Thompson", "larry.thompson@company.com"},
                {"maria.garcia", "Maria", "Garcia", "maria.garcia@company.com"},
                {"nathan.martinez", "Nathan", "Martinez", "nathan.martinez@company.com"},
                {"olivia.robinson", "Olivia", "Robinson", "olivia.robinson@company.com"},
                {"paul.clark", "Paul", "Clark", "paul.clark@company.com"},
                {"quinn.rodriguez", "Quinn", "Rodriguez", "quinn.rodriguez@company.com"},
                {"rachel.lewis", "Rachel", "Lewis", "rachel.lewis@company.com"},
                {"samuel.lee", "Samuel", "Lee", "samuel.lee@company.com"},
                {"tina.walker", "Tina", "Walker", "tina.walker@company.com"},
                {"ulrich.hall", "Ulrich", "Hall", "ulrich.hall@company.com"},
                {"victoria.allen", "Victoria", "Allen", "victoria.allen@company.com"},
                {"walter.young", "Walter", "Young", "walter.young@company.com"},
                {"xena.king", "Xena", "King", "xena.king@company.com"},
                {"yves.wright", "Yves", "Wright", "yves.wright@company.com"},
                {"zoe.lopez", "Zoe", "Lopez", "zoe.lopez@company.com"},
                {"adam.hill", "Adam", "Hill", "adam.hill@company.com"},
                {"beth.scott", "Beth", "Scott", "beth.scott@company.com"},
                {"carl.green", "Carl", "Green", "carl.green@company.com"},
                {"diana.adams", "Diana", "Adams", "diana.adams@company.com"},
                {"edward.baker", "Edward", "Baker", "edward.baker@company.com"},
                {"fiona.gonzalez", "Fiona", "Gonzalez", "fiona.gonzalez@company.com"},
                {"george.nelson", "George", "Nelson", "george.nelson@company.com"},
                {"helen.carter", "Helen", "Carter", "helen.carter@company.com"},
                {"ian.mitchell", "Ian", "Mitchell", "ian.mitchell@company.com"},
                {"julia.perez", "Julia", "Perez", "julia.perez@company.com"},
                {"keith.thompson", "Keith", "Thompson", "keith.thompson@company.com"},
                {"linda.garcia", "Linda", "Garcia", "linda.garcia@company.com"},
                {"mark.martinez", "Mark", "Martinez", "mark.martinez@company.com"},
                {"nancy.robinson", "Nancy", "Robinson", "nancy.robinson@company.com"},
                {"oscar.clark", "Oscar", "Clark", "oscar.clark@company.com"},
                {"pamela.rodriguez", "Pamela", "Rodriguez", "pamela.rodriguez@company.com"},
                
                // Finance Users (35 users)
                {"alex.turner", "Alex", "Turner", "alex.turner@company.com"},
                {"bella.campbell", "Bella", "Campbell", "bella.campbell@company.com"},
                {"chris.parker", "Chris", "Parker", "chris.parker@company.com"},
                {"daisy.evans", "Daisy", "Evans", "daisy.evans@company.com"},
                {"evan.edwards", "Evan", "Edwards", "evan.edwards@company.com"},
                {"faye.collins", "Faye", "Collins", "faye.collins@company.com"},
                {"gavin.stewart", "Gavin", "Stewart", "gavin.stewart@company.com"},
                {"hannah.sanchez", "Hannah", "Sanchez", "hannah.sanchez@company.com"},
                {"ivan.morris", "Ivan", "Morris", "ivan.morris@company.com"},
                {"jade.rogers", "Jade", "Rogers", "jade.rogers@company.com"},
                {"kyle.reed", "Kyle", "Reed", "kyle.reed@company.com"},
                {"luna.cook", "Luna", "Cook", "luna.cook@company.com"},
                {"mason.morgan", "Mason", "Morgan", "mason.morgan@company.com"},
                {"nora.bell", "Nora", "Bell", "nora.bell@company.com"},
                {"owen.murphy", "Owen", "Murphy", "owen.murphy@company.com"},
                {"piper.bailey", "Piper", "Bailey", "piper.bailey@company.com"},
                {"quinn.rivera", "Quinn", "Rivera", "quinn.rivera@company.com"},
                {"ruby.cooper", "Ruby", "Cooper", "ruby.cooper@company.com"},
                {"seth.richardson", "Seth", "Richardson", "seth.richardson@company.com"},
                {"tara.cox", "Tara", "Cox", "tara.cox@company.com"},
                {"ulysses.ward", "Ulysses", "Ward", "ulysses.ward@company.com"},
                {"violet.torres", "Violet", "Torres", "violet.torres@company.com"},
                {"wade.peterson", "Wade", "Peterson", "wade.peterson@company.com"},
                {"xander.gray", "Xander", "Gray", "xander.gray@company.com"},
                {"yara.ramirez", "Yara", "Ramirez", "yara.ramirez@company.com"},
                {"zane.james", "Zane", "James", "zane.james@company.com"},
                {"ava.watson", "Ava", "Watson", "ava.watson@company.com"},
                {"blake.brooks", "Blake", "Brooks", "blake.brooks@company.com"},
                {"cora.kelly", "Cora", "Kelly", "cora.kelly@company.com"},
                {"drew.sanders", "Drew", "Sanders", "drew.sanders@company.com"},
                {"eve.price", "Eve", "Price", "eve.price@company.com"},
                {"finn.bennett", "Finn", "Bennett", "finn.bennett@company.com"},
                {"gigi.wood", "Gigi", "Wood", "gigi.wood@company.com"},
                {"hudson.barnes", "Hudson", "Barnes", "hudson.barnes@company.com"},
                {"iris.ross", "Iris", "Ross", "iris.ross@company.com"},
                {"jett.henderson", "Jett", "Henderson", "jett.henderson@company.com"},
                {"kate.coleman", "Kate", "Coleman", "kate.coleman@company.com"},
                {"leo.jenkins", "Leo", "Jenkins", "leo.jenkins@company.com"},
                {"maya.perry", "Maya", "Perry", "maya.perry@company.com"},
                {"nash.powell", "Nash", "Powell", "nash.powell@company.com"},
                {"opal.long", "Opal", "Long", "opal.long@company.com"},
                {"pax.patterson", "Pax", "Patterson", "pax.patterson@company.com"},
                {"quinn.hughes", "Quinn", "Hughes", "quinn.hughes@company.com"},
                {"rory.flores", "Rory", "Flores", "rory.flores@company.com"},
                {"sage.washington", "Sage", "Washington", "sage.washington@company.com"},
                {"tate.butler", "Tate", "Butler", "tate.butler@company.com"},
                {"una.simmons", "Una", "Simmons", "una.simmons@company.com"},
                {"vance.foster", "Vance", "Foster", "vance.foster@company.com"},
                {"willa.gonzales", "Willa", "Gonzales", "willa.gonzales@company.com"},
                {"xavi.bryant", "Xavi", "Bryant", "xavi.bryant@company.com"},
                {"yuki.alexander", "Yuki", "Alexander", "yuki.alexander@company.com"},
                {"zara.russell", "Zara", "Russell", "zara.russell@company.com"}
            };

        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        int createdCount = 0;
        int existingCount = 0;

        for (String[] user : users) {
            String username = user[0];
            String firstName = user[1];
            String lastName = user[2];
            String email = user[3];
            
            try {
                if (!personService.personExists(username)) {
                    // Create person properties map
                    Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
                    properties.put(ContentModel.PROP_USERNAME, username);
                    properties.put(ContentModel.PROP_FIRSTNAME, firstName);
                    properties.put(ContentModel.PROP_LASTNAME, lastName);
                    properties.put(ContentModel.PROP_EMAIL, email);
                    
                    personService.createPerson(properties);
                    
                    Map<String, String> result = new HashMap<String, String>();
                    result.put("username", username);
                    result.put("status", "created");
                    result.put("message", "User created successfully");
                    results.add(result);
                    createdCount++;
                    
                    logger.info("Created user: " + username);
                } else {
                    Map<String, String> result = new HashMap<String, String>();
                    result.put("username", username);
                    result.put("status", "exists");
                    result.put("message", "User already exists");
                    results.add(result);
                    existingCount++;
                    
                    logger.info("User already exists: " + username);
                }
            } catch (Exception e) {
                Map<String, String> result = new HashMap<String, String>();
                result.put("username", username);
                result.put("status", "error");
                result.put("message", "Error creating user: " + e.getMessage());
                results.add(result);
                
                logger.error("Error creating user " + username + ": " + e.getMessage(), e);
            }
        }

        model.put("results", results);
        model.put("createdCount", createdCount);
        model.put("existingCount", existingCount);
        model.put("totalUsers", users.length);

        return model;
    }
}
