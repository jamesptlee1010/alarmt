package com.james.mathwakealarm

import kotlin.math.abs
import kotlin.random.Random

object QuestionEngine {
    data class Question(
        val id: String,
        val topic: Topic,
        val prompt: String,
        val answer: String,
        val acceptedAnswers: List<String> = emptyList()
    )

    private val fixedQuestions = listOf(
        Question("ww2_01", Topic.WORLD_WAR_II, "In what year did World War II begin in Europe?", "1939"),
        Question("ww2_02", Topic.WORLD_WAR_II, "Which country did Germany invade in September 1939?", "Poland"),
        Question("ww2_03", Topic.WORLD_WAR_II, "What was the code name for the Allied invasion of Normandy?", "Operation Overlord", listOf("Overlord")),
        Question("ww2_04", Topic.WORLD_WAR_II, "On what date did D-Day occur?", "6 June 1944", listOf("June 6 1944", "6/6/1944")),
        Question("ww2_05", Topic.WORLD_WAR_II, "Which battle is widely considered the turning point on the Eastern Front?", "Stalingrad", listOf("Battle of Stalingrad")),
        Question("ww2_06", Topic.WORLD_WAR_II, "Who was British prime minister for most of World War II?", "Winston Churchill", listOf("Churchill")),
        Question("ww2_07", Topic.WORLD_WAR_II, "What was the name of Germany's air force?", "Luftwaffe"),
        Question("ww2_08", Topic.WORLD_WAR_II, "Which 1941 attack brought the United States into World War II?", "Pearl Harbor", listOf("Attack on Pearl Harbor")),
        Question("ww2_09", Topic.WORLD_WAR_II, "Which city was the first target of an atomic bomb in war?", "Hiroshima"),
        Question("ww2_10", Topic.WORLD_WAR_II, "In what year did World War II end?", "1945"),

        Question("jung_01", Topic.CARL_JUNG, "Carl Jung founded which school of psychology?", "Analytical psychology"),
        Question("jung_02", Topic.CARL_JUNG, "What term did Jung use for universal inherited patterns?", "Archetypes", listOf("Archetype")),
        Question("jung_03", Topic.CARL_JUNG, "What is Jung's term for the hidden, rejected side of personality?", "Shadow", listOf("The shadow")),
        Question("jung_04", Topic.CARL_JUNG, "What did Jung call the process of becoming a whole self?", "Individuation"),
        Question("jung_05", Topic.CARL_JUNG, "Which two broad attitudes did Jung describe?", "Introversion and extraversion", listOf("Introvert and extrovert", "Introversion and extroversion")),
        Question("jung_06", Topic.CARL_JUNG, "What is Jung's term for humanity's shared inherited unconscious?", "Collective unconscious", listOf("The collective unconscious")),
        Question("jung_07", Topic.CARL_JUNG, "Which Swiss city is closely associated with Jung's career?", "Zurich", listOf("Zürich")),
        Question("jung_08", Topic.CARL_JUNG, "What was Carl Jung's profession?", "Psychiatrist", listOf("Psychologist", "Psychiatrist and psychoanalyst")),

        Question("c20_01", Topic.TWENTIETH_CENTURY, "In what year did the Berlin Wall fall?", "1989"),
        Question("c20_02", Topic.TWENTIETH_CENTURY, "Who was the first person to walk on the Moon?", "Neil Armstrong", listOf("Armstrong")),
        Question("c20_03", Topic.TWENTIETH_CENTURY, "In what year did the Soviet Union dissolve?", "1991"),
        Question("c20_04", Topic.TWENTIETH_CENTURY, "Which crisis in 1962 brought the US and USSR close to nuclear war?", "Cuban Missile Crisis"),
        Question("c20_05", Topic.TWENTIETH_CENTURY, "Who delivered the 'I Have a Dream' speech?", "Martin Luther King Jr", listOf("Martin Luther King", "MLK")),
        Question("c20_06", Topic.TWENTIETH_CENTURY, "Which ship sank on its maiden voyage in 1912?", "Titanic", listOf("RMS Titanic")),
        Question("c20_07", Topic.TWENTIETH_CENTURY, "What year did humans first land on the Moon?", "1969"),
        Question("c20_08", Topic.TWENTIETH_CENTURY, "Which South African leader became president in 1994?", "Nelson Mandela", listOf("Mandela")),

        Question("geo_01", Topic.GEOGRAPHY, "What is the capital of Australia?", "Canberra"),
        Question("geo_02", Topic.GEOGRAPHY, "What is the largest ocean on Earth?", "Pacific Ocean", listOf("Pacific")),
        Question("geo_03", Topic.GEOGRAPHY, "Which river flows through London?", "Thames", listOf("River Thames")),
        Question("geo_04", Topic.GEOGRAPHY, "Mount Kilimanjaro is in which country?", "Tanzania"),
        Question("geo_05", Topic.GEOGRAPHY, "What is the capital of Japan?", "Tokyo"),
        Question("geo_06", Topic.GEOGRAPHY, "Which continent contains the Sahara Desert?", "Africa"),
        Question("geo_07", Topic.GEOGRAPHY, "What is the smallest Australian state by area?", "Tasmania"),
        Question("geo_08", Topic.GEOGRAPHY, "Which country has the city of Barcelona?", "Spain"),

        Question("sci_01", Topic.SCIENCE, "What planet is known as the Red Planet?", "Mars"),
        Question("sci_02", Topic.SCIENCE, "What gas do plants absorb from the atmosphere?", "Carbon dioxide", listOf("CO2")),
        Question("sci_03", Topic.SCIENCE, "How many bones are in a typical adult human body?", "206"),
        Question("sci_04", Topic.SCIENCE, "What is the chemical symbol for gold?", "Au"),
        Question("sci_05", Topic.SCIENCE, "What force keeps planets in orbit?", "Gravity"),
        Question("sci_06", Topic.SCIENCE, "What is H2O commonly called?", "Water"),
        Question("sci_07", Topic.SCIENCE, "Which organ pumps blood around the body?", "Heart", listOf("The heart")),
        Question("sci_08", Topic.SCIENCE, "What is the nearest star to Earth?", "Sun", listOf("The Sun")),

        Question("sport_01", Topic.SPORT, "How many players does a football team have on the pitch at kick-off?", "11", listOf("Eleven")),
        Question("sport_02", Topic.SPORT, "How long is an Olympic swimming pool in metres?", "50", listOf("50 metres", "50m")),
        Question("sport_03", Topic.SPORT, "In tennis, what word means a score of zero?", "Love"),
        Question("sport_04", Topic.SPORT, "How many points is a try worth in rugby union?", "5", listOf("Five")),
        Question("sport_05", Topic.SPORT, "Which country hosted the 2000 Summer Olympics?", "Australia", listOf("Sydney", "Australia Sydney")),
        Question("sport_06", Topic.SPORT, "What colour jersey is worn by the Tour de France leader?", "Yellow"),

        Question("gk_01", Topic.GENERAL_KNOWLEDGE, "How many days are in a leap year?", "366"),
        Question("gk_02", Topic.GENERAL_KNOWLEDGE, "Which instrument has 88 keys?", "Piano"),
        Question("gk_03", Topic.GENERAL_KNOWLEDGE, "What is the currency of Japan?", "Yen"),
        Question("gk_04", Topic.GENERAL_KNOWLEDGE, "Who wrote 1984?", "George Orwell", listOf("Orwell")),
        Question("gk_05", Topic.GENERAL_KNOWLEDGE, "How many sides does a hexagon have?", "6", listOf("Six")),
        Question("gk_06", Topic.GENERAL_KNOWLEDGE, "What colour do blue and yellow make?", "Green"),

        Question("logic_01", Topic.LOGIC, "Complete the sequence: 2, 4, 8, 16, ?", "32"),
        Question("logic_02", Topic.LOGIC, "Complete the sequence: 1, 1, 2, 3, 5, ?", "8"),
        Question("logic_03", Topic.LOGIC, "If all cats are animals and Taz is a cat, is Taz an animal?", "Yes", listOf("Y")),
        Question("logic_04", Topic.LOGIC, "What comes next: A, C, E, G, ?", "I"),
        Question("logic_05", Topic.LOGIC, "If yesterday was Monday, what day is tomorrow?", "Wednesday"),

        Question("dm_01", Topic.DANCE_MOMS, "Which dance teacher is at the centre of Dance Moms?", "Abby Lee Miller", listOf("Abby")),
        Question("dm_02", Topic.DANCE_MOMS, "What does ALDC stand for?", "Abby Lee Dance Company", listOf("ALDC")),
        Question("dm_03", Topic.DANCE_MOMS, "Which city is the Abby Lee Dance Company most associated with in the early series?", "Pittsburgh"),
        Question("dm_04", Topic.DANCE_MOMS, "Which network originally aired Dance Moms?", "Lifetime"),
        Question("dm_05", Topic.DANCE_MOMS, "What is the first name of Maddie Ziegler's younger sister?", "Mackenzie", listOf("Kenzie")),
        Question("dm_06", Topic.DANCE_MOMS, "Which mom is the mother of Maddie and Mackenzie Ziegler?", "Melissa", listOf("Melissa Gisoni")),
        Question("dm_07", Topic.DANCE_MOMS, "What is the first name of Chloe Lukasiak's mother?", "Christi", listOf("Christi Lukasiak")),
        Question("dm_08", Topic.DANCE_MOMS, "Which mom is the mother of Nia Sioux?", "Holly", listOf("Holly Frazier")),
        Question("dm_09", Topic.DANCE_MOMS, "Which mom is the mother of Brooke and Paige Hyland?", "Kelly", listOf("Kelly Hyland")),
        Question("dm_10", Topic.DANCE_MOMS, "Which mom is the mother of Kendall Vertes?", "Jill", listOf("Jill Vertes")),
        Question("dm_11", Topic.DANCE_MOMS, "Which mom is the mother of Kalani Hilliker?", "Kira", listOf("Kira Girard")),
        Question("dm_12", Topic.DANCE_MOMS, "Which mom is the mother of JoJo Siwa?", "Jessalynn", listOf("Jess", "Jessalynn Siwa")),
        Question("dm_13", Topic.DANCE_MOMS, "Which mom is the mother of Brynn Rumfallo?", "Ashlee", listOf("Ashlee Allen")),
        Question("dm_14", Topic.DANCE_MOMS, "Which mom is the mother of Elliana Walmsley?", "Yolanda", listOf("Yolanda Walmsley")),
        Question("dm_15", Topic.DANCE_MOMS, "Which mom is the mother of Lilliana Ketchman?", "Stacey", listOf("Stacey Ketchman")),
        Question("dm_16", Topic.DANCE_MOMS, "Which mom is the mother of Vivi-Anne Stein?", "Cathy", listOf("Cathy Nesbitt-Stein")),
        Question("dm_17", Topic.DANCE_MOMS, "What is the name of Cathy's rival dance studio?", "Candy Apples Dance Center", listOf("Candy Apples", "Candy Apples Dance Centre")),
        Question("dm_18", Topic.DANCE_MOMS, "Who owns the Candy Apples Dance Center on Dance Moms?", "Cathy Nesbitt-Stein", listOf("Cathy")),
        Question("dm_19", Topic.DANCE_MOMS, "What display did Abby use to rank dancers at the start of many episodes?", "The pyramid", listOf("Pyramid")),
        Question("dm_20", Topic.DANCE_MOMS, "What was Abby's main competition team commonly called?", "Junior Elite Competition Team", listOf("Junior Elite Team", "ALDC Junior Elite Competition Team")),
        Question("dm_21", Topic.DANCE_MOMS, "What type of dance includes the whole team performing together?", "Group dance", listOf("Group")),
        Question("dm_22", Topic.DANCE_MOMS, "What is a dance performed by one dancer called?", "Solo"),
        Question("dm_23", Topic.DANCE_MOMS, "What is a dance performed by two dancers called?", "Duet"),
        Question("dm_24", Topic.DANCE_MOMS, "What is a dance performed by three dancers called?", "Trio"),
        Question("dm_25", Topic.DANCE_MOMS, "What is Maddie's surname?", "Ziegler"),
        Question("dm_26", Topic.DANCE_MOMS, "What is Mackenzie's surname?", "Ziegler"),
        Question("dm_27", Topic.DANCE_MOMS, "What is Chloe's surname?", "Lukasiak"),
        Question("dm_28", Topic.DANCE_MOMS, "What surname did Nia use professionally on the show?", "Sioux", listOf("Frazier")),
        Question("dm_29", Topic.DANCE_MOMS, "What is Brooke's surname?", "Hyland"),
        Question("dm_30", Topic.DANCE_MOMS, "What is Paige's surname?", "Hyland"),
        Question("dm_31", Topic.DANCE_MOMS, "What is Kendall's surname?", "Vertes"),
        Question("dm_32", Topic.DANCE_MOMS, "What is Kalani's surname?", "Hilliker"),
        Question("dm_33", Topic.DANCE_MOMS, "What is JoJo's surname?", "Siwa"),
        Question("dm_34", Topic.DANCE_MOMS, "What is Brynn's surname?", "Rumfallo"),
        Question("dm_35", Topic.DANCE_MOMS, "What is Elliana's surname?", "Walmsley"),
        Question("dm_36", Topic.DANCE_MOMS, "What is Lilliana's surname?", "Ketchman"),
        Question("dm_37", Topic.DANCE_MOMS, "What is Vivi-Anne's surname?", "Stein"),
        Question("dm_38", Topic.DANCE_MOMS, "Which Hyland sister is older, Brooke or Paige?", "Brooke"),
        Question("dm_39", Topic.DANCE_MOMS, "Which Hyland sister is younger, Brooke or Paige?", "Paige"),
        Question("dm_40", Topic.DANCE_MOMS, "Which Ziegler sister is older, Maddie or Mackenzie?", "Maddie"),
        Question("dm_41", Topic.DANCE_MOMS, "Which Ziegler sister is younger, Maddie or Mackenzie?", "Mackenzie", listOf("Kenzie")),
        Question("dm_42", Topic.DANCE_MOMS, "Which Dance Moms dancer became especially known for wearing large hair bows?", "JoJo Siwa", listOf("JoJo")),
        Question("dm_43", Topic.DANCE_MOMS, "What nickname is commonly used for Mackenzie Ziegler?", "Kenzie", listOf("Mackenzie")),
        Question("dm_44", Topic.DANCE_MOMS, "Which dancer's mother is named Christi?", "Chloe", listOf("Chloe Lukasiak")),
        Question("dm_45", Topic.DANCE_MOMS, "Which dancer's mother is named Holly?", "Nia", listOf("Nia Sioux")),
        Question("dm_46", Topic.DANCE_MOMS, "Which dancer's mother is named Jill?", "Kendall", listOf("Kendall Vertes")),
        Question("dm_47", Topic.DANCE_MOMS, "Which rival studio owner frequently competed against Abby's team?", "Cathy", listOf("Cathy Nesbitt-Stein")),
        Question("dm_48", Topic.DANCE_MOMS, "Which dancer with the surname Lukasiak competed with the ALDC?", "Chloe", listOf("Chloe Lukasiak")),
        Question("dm_49", Topic.DANCE_MOMS, "Which dancer with the surname Sioux competed with the ALDC?", "Nia", listOf("Nia Sioux")),
        Question("dm_50", Topic.DANCE_MOMS, "Which dancer with the surname Hyland is Paige's older sister?", "Brooke", listOf("Brooke Hyland")),

        Question("tm2_01", Topic.TEEN_MOM_2, "Which network aired Teen Mom 2?", "MTV"),
        Question("tm2_02", Topic.TEEN_MOM_2, "Teen Mom 2 was developed from which earlier MTV series?", "16 and Pregnant", listOf("16 & Pregnant")),
        Question("tm2_03", Topic.TEEN_MOM_2, "Name one of the four original Teen Mom 2 mothers.", "Chelsea", listOf("Chelsea Houska", "Jenelle", "Jenelle Evans", "Kailyn", "Kailyn Lowry", "Leah", "Leah Messer")),
        Question("tm2_04", Topic.TEEN_MOM_2, "Which Teen Mom 2 cast member is the mother of Aubree?", "Chelsea", listOf("Chelsea Houska", "Chelsea DeBoer")),
        Question("tm2_05", Topic.TEEN_MOM_2, "Who is Aubree's father?", "Adam Lind", listOf("Adam")),
        Question("tm2_06", Topic.TEEN_MOM_2, "Which cast member married Cole DeBoer?", "Chelsea", listOf("Chelsea Houska", "Chelsea DeBoer")),
        Question("tm2_07", Topic.TEEN_MOM_2, "Which cast member is the mother of Watson?", "Chelsea", listOf("Chelsea Houska", "Chelsea DeBoer")),
        Question("tm2_08", Topic.TEEN_MOM_2, "Which cast member is the mother of Layne?", "Chelsea", listOf("Chelsea Houska", "Chelsea DeBoer")),
        Question("tm2_09", Topic.TEEN_MOM_2, "Which cast member is the mother of Walker?", "Chelsea", listOf("Chelsea Houska", "Chelsea DeBoer")),
        Question("tm2_10", Topic.TEEN_MOM_2, "Which Teen Mom 2 cast member is the mother of Jace?", "Jenelle", listOf("Jenelle Evans")),
        Question("tm2_11", Topic.TEEN_MOM_2, "What is the first name of Jenelle's mother?", "Barbara", listOf("Barb")),
        Question("tm2_12", Topic.TEEN_MOM_2, "Which cast member is the mother of Kaiser?", "Jenelle", listOf("Jenelle Evans")),
        Question("tm2_13", Topic.TEEN_MOM_2, "Which cast member is the mother of Ensley?", "Jenelle", listOf("Jenelle Evans")),
        Question("tm2_14", Topic.TEEN_MOM_2, "Which cast member married David Eason?", "Jenelle", listOf("Jenelle Evans")),
        Question("tm2_15", Topic.TEEN_MOM_2, "Which Teen Mom 2 cast member is the mother of Isaac?", "Kailyn", listOf("Kail", "Kailyn Lowry")),
        Question("tm2_16", Topic.TEEN_MOM_2, "Who is Isaac's father?", "Jo Rivera", listOf("Jo")),
        Question("tm2_17", Topic.TEEN_MOM_2, "Which cast member is the mother of Lincoln?", "Kailyn", listOf("Kail", "Kailyn Lowry")),
        Question("tm2_18", Topic.TEEN_MOM_2, "Who is Lincoln's father?", "Javi Marroquin", listOf("Javi")),
        Question("tm2_19", Topic.TEEN_MOM_2, "Which cast member is the mother of Lux?", "Kailyn", listOf("Kail", "Kailyn Lowry")),
        Question("tm2_20", Topic.TEEN_MOM_2, "Which cast member is the mother of Creed?", "Kailyn", listOf("Kail", "Kailyn Lowry")),
        Question("tm2_21", Topic.TEEN_MOM_2, "What is Kailyn's surname?", "Lowry"),
        Question("tm2_22", Topic.TEEN_MOM_2, "Which cast member has twins named Ali and Aleeah?", "Leah", listOf("Leah Messer")),
        Question("tm2_23", Topic.TEEN_MOM_2, "Which cast member is the mother of Addie?", "Leah", listOf("Leah Messer")),
        Question("tm2_24", Topic.TEEN_MOM_2, "Who is the father of Leah's twins?", "Corey Simms", listOf("Corey")),
        Question("tm2_25", Topic.TEEN_MOM_2, "Who is Addie's father?", "Jeremy Calvert", listOf("Jeremy")),
        Question("tm2_26", Topic.TEEN_MOM_2, "What is Leah's surname?", "Messer"),
        Question("tm2_27", Topic.TEEN_MOM_2, "What surname was Chelsea known by in the early series?", "Houska"),
        Question("tm2_28", Topic.TEEN_MOM_2, "What is Jenelle's surname?", "Evans"),
        Question("tm2_29", Topic.TEEN_MOM_2, "Which later Teen Mom 2 cast member has the surname DeJesus?", "Briana", listOf("Briana DeJesus")),
        Question("tm2_30", Topic.TEEN_MOM_2, "Which cast member is the mother of Nova and Stella?", "Briana", listOf("Briana DeJesus")),
        Question("tm2_31", Topic.TEEN_MOM_2, "Which of Briana's daughters is older, Nova or Stella?", "Nova"),
        Question("tm2_32", Topic.TEEN_MOM_2, "Which of Briana's daughters is younger, Nova or Stella?", "Stella"),
        Question("tm2_33", Topic.TEEN_MOM_2, "Which later cast member has the surname Cline?", "Jade", listOf("Jade Cline")),
        Question("tm2_34", Topic.TEEN_MOM_2, "What is the first name of Jade Cline's daughter?", "Kloie"),
        Question("tm2_35", Topic.TEEN_MOM_2, "Which later cast member has the surname Jones?", "Ashley", listOf("Ashley Jones")),
        Question("tm2_36", Topic.TEEN_MOM_2, "What is the first name of Ashley Jones's daughter?", "Holly"),
        Question("tm2_37", Topic.TEEN_MOM_2, "Is Teen Mom 2 a scripted drama or a reality series?", "Reality series", listOf("Reality TV", "Reality show")),
        Question("tm2_38", Topic.TEEN_MOM_2, "Which original cast member is most associated with South Dakota?", "Chelsea", listOf("Chelsea Houska", "Chelsea DeBoer")),
        Question("tm2_39", Topic.TEEN_MOM_2, "Which original cast member is most associated with North Carolina?", "Jenelle", listOf("Jenelle Evans")),
        Question("tm2_40", Topic.TEEN_MOM_2, "Which original cast member lived in Delaware during much of the series?", "Kailyn", listOf("Kail", "Kailyn Lowry")),
        Question("tm2_41", Topic.TEEN_MOM_2, "Which original cast member is most associated with West Virginia?", "Leah", listOf("Leah Messer")),
        Question("tm2_42", Topic.TEEN_MOM_2, "What is the first name of Chelsea's father?", "Randy", listOf("Randy Houska")),
        Question("tm2_43", Topic.TEEN_MOM_2, "Who raised Jace for much of his childhood on the show?", "Barbara", listOf("Barb", "Barbara Evans")),
        Question("tm2_44", Topic.TEEN_MOM_2, "Which man is strongly associated with Kailyn's early co-parenting storyline?", "Jo Rivera", listOf("Jo")),
        Question("tm2_45", Topic.TEEN_MOM_2, "Which man is strongly associated with Leah's twin-daughters storyline?", "Corey Simms", listOf("Corey")),
        Question("tm2_46", Topic.TEEN_MOM_2, "Which man is Aubree's biological father?", "Adam Lind", listOf("Adam")),
        Question("tm2_47", Topic.TEEN_MOM_2, "Which man is Kaiser's father?", "Nathan Griffith", listOf("Nathan")),
        Question("tm2_48", Topic.TEEN_MOM_2, "What is Cole's surname?", "DeBoer"),
        Question("tm2_49", Topic.TEEN_MOM_2, "What is Jeremy's surname?", "Calvert"),
        Question("tm2_50", Topic.TEEN_MOM_2, "What number appears in the title Teen Mom 2?", "2", listOf("Two")),

        Question("mw_01", Topic.MORMON_WIVES, "What is the full title of the Hulu reality series about MomTok?", "The Secret Lives of Mormon Wives", listOf("Secret Lives of Mormon Wives")),
        Question("mw_02", Topic.MORMON_WIVES, "Which streaming service is the home of The Secret Lives of Mormon Wives?", "Hulu"),
        Question("mw_03", Topic.MORMON_WIVES, "What is the social-media group featured in The Secret Lives of Mormon Wives commonly called?", "MomTok", listOf("Mom Tok")),
        Question("mw_04", Topic.MORMON_WIVES, "Which cast member is described by Hulu as the creator of #MomTok?", "Taylor Frankie Paul", listOf("Taylor Paul", "Taylor")),
        Question("mw_05", Topic.MORMON_WIVES, "Which cast member was at the centre of the swinging scandal that launched the show's premise?", "Taylor Frankie Paul", listOf("Taylor Paul", "Taylor")),
        Question("mw_06", Topic.MORMON_WIVES, "What is the first name of Taylor Frankie Paul's boyfriend in the original Hulu bio?", "Dakota", listOf("Dakota Mortensen")),
        Question("mw_07", Topic.MORMON_WIVES, "Which cast member gave birth to her third child with Dakota?", "Taylor Frankie Paul", listOf("Taylor Paul", "Taylor")),
        Question("mw_08", Topic.MORMON_WIVES, "Which cast member has the surname Engemann?", "Demi", listOf("Demi Engemann")),
        Question("mw_09", Topic.MORMON_WIVES, "Which cast member advocates for ketamine therapy as an aid for mental-health healing?", "Demi Engemann", listOf("Demi")),
        Question("mw_10", Topic.MORMON_WIVES, "Which cast member's Hulu bio describes a blended family with three children?", "Demi Engemann", listOf("Demi", "Jessi Ngatikaura", "Jessi")),
        Question("mw_11", Topic.MORMON_WIVES, "Which cast member has the surname Affleck?", "Jennifer", listOf("Jennifer Affleck", "Jen Affleck", "Jen")),
        Question("mw_12", Topic.MORMON_WIVES, "What is the first name of Jennifer Affleck's husband?", "Zac", listOf("Zac Affleck")),
        Question("mw_13", Topic.MORMON_WIVES, "Jennifer Affleck's husband is a cousin of which famous Affleck brothers?", "Ben and Casey Affleck", listOf("Ben Affleck and Casey Affleck", "Ben and Casey")),
        Question("mw_14", Topic.MORMON_WIVES, "Which cast member is known for dancing on her countertops on TikTok?", "Jennifer Affleck", listOf("Jen Affleck", "Jennifer", "Jen")),
        Question("mw_15", Topic.MORMON_WIVES, "Which cast member has the surname Ngatikaura?", "Jessi", listOf("Jessi Ngatikaura")),
        Question("mw_16", Topic.MORMON_WIVES, "What is the name of Jessi Ngatikaura's hair business?", "JZ Styles", listOf("JZ")),
        Question("mw_17", Topic.MORMON_WIVES, "Which cast member owns a hair school and extension company?", "Jessi Ngatikaura", listOf("Jessi")),
        Question("mw_18", Topic.MORMON_WIVES, "Which cast member calls herself the grandma of the group?", "Jessi Ngatikaura", listOf("Jessi")),
        Question("mw_19", Topic.MORMON_WIVES, "Which cast member's Hulu bio describes a remarried blended family with three kids?", "Jessi Ngatikaura", listOf("Jessi", "Demi Engemann", "Demi")),
        Question("mw_20", Topic.MORMON_WIVES, "Which cast member has the surname Taylor?", "Layla", listOf("Layla Taylor")),
        Question("mw_21", Topic.MORMON_WIVES, "Which cast member's Hulu bio says she loves the Utah soda shop Swig?", "Layla Taylor", listOf("Layla")),
        Question("mw_22", Topic.MORMON_WIVES, "Which cast member was described as a newly divorced single mother of two boys?", "Layla Taylor", listOf("Layla")),
        Question("mw_23", Topic.MORMON_WIVES, "Which cast member has the surname Neeley?", "Mayci", listOf("Mayci Neeley")),
        Question("mw_24", Topic.MORMON_WIVES, "Which cast member played Division 1 tennis?", "Mayci Neeley", listOf("Mayci")),
        Question("mw_25", Topic.MORMON_WIVES, "At which university did Mayci Neeley play Division 1 tennis?", "BYU", listOf("Brigham Young University")),
        Question("mw_26", Topic.MORMON_WIVES, "Which cast member founded Baby Mama?", "Mayci Neeley", listOf("Mayci")),
        Question("mw_27", Topic.MORMON_WIVES, "What type of company is Mayci Neeley's Baby Mama brand?", "Natal nutrition company", listOf("Nutrition company", "Prenatal nutrition company")),
        Question("mw_28", Topic.MORMON_WIVES, "Which cast member discussed an IVF journey in her Hulu bio?", "Mayci Neeley", listOf("Mayci")),
        Question("mw_29", Topic.MORMON_WIVES, "Which cast member has the surname Matthews?", "Mikayla", listOf("Mikayla Matthews")),
        Question("mw_30", Topic.MORMON_WIVES, "Which cast member has openly advocated for skincare by sharing her own skin struggles?", "Mikayla Matthews", listOf("Mikayla")),
        Question("mw_31", Topic.MORMON_WIVES, "Which cast member's Hulu bio says she was a teen mom?", "Mikayla Matthews", listOf("Mikayla")),
        Question("mw_32", Topic.MORMON_WIVES, "Which cast member's Hulu bio describes her as married with three children?", "Mikayla Matthews", listOf("Mikayla")),
        Question("mw_33", Topic.MORMON_WIVES, "Which cast member has the surname Leavitt?", "Whitney", listOf("Whitney Leavitt")),
        Question("mw_34", Topic.MORMON_WIVES, "Which cast member dreams of having a homestead with her family?", "Whitney Leavitt", listOf("Whitney")),
        Question("mw_35", Topic.MORMON_WIVES, "Which cast member said the group had built a business together before the show?", "Whitney Leavitt", listOf("Whitney")),
        Question("mw_36", Topic.MORMON_WIVES, "How many women are listed in Hulu's core cast bios for the original show?", "8", listOf("Eight")),
        Question("mw_37", Topic.MORMON_WIVES, "Is Taylor Frankie Paul part of the core Hulu cast?", "Yes", listOf("Y")),
        Question("mw_38", Topic.MORMON_WIVES, "Is Demi Engemann part of the core Hulu cast?", "Yes", listOf("Y")),
        Question("mw_39", Topic.MORMON_WIVES, "Is Jennifer Affleck part of the core Hulu cast?", "Yes", listOf("Y")),
        Question("mw_40", Topic.MORMON_WIVES, "Is Jessi Ngatikaura part of the core Hulu cast?", "Yes", listOf("Y")),
        Question("mw_41", Topic.MORMON_WIVES, "Is Layla Taylor part of the core Hulu cast?", "Yes", listOf("Y")),
        Question("mw_42", Topic.MORMON_WIVES, "Is Mayci Neeley part of the core Hulu cast?", "Yes", listOf("Y")),
        Question("mw_43", Topic.MORMON_WIVES, "Is Mikayla Matthews part of the core Hulu cast?", "Yes", listOf("Y")),
        Question("mw_44", Topic.MORMON_WIVES, "Is Whitney Leavitt part of the core Hulu cast?", "Yes", listOf("Y")),
        Question("mw_45", Topic.MORMON_WIVES, "Which US state is most closely associated with the MomTok group in the show?", "Utah"),
        Question("mw_46", Topic.MORMON_WIVES, "Does the title use the word Mormon or Catholic?", "Mormon"),
        Question("mw_47", Topic.MORMON_WIVES, "Which hashtag is central to the group's online identity?", "#MomTok", listOf("MomTok", "Mom Tok")),
        Question("mw_48", Topic.MORMON_WIVES, "Is The Secret Lives of Mormon Wives a reality series or a fictional sitcom?", "Reality series", listOf("Reality TV", "Reality show")),
        Question("mw_49", Topic.MORMON_WIVES, "Which cast member's first and middle names are Taylor Frankie?", "Taylor Frankie Paul", listOf("Taylor Paul", "Taylor")),
        Question("mw_50", Topic.MORMON_WIVES, "Which cast member's first name is Whitney?", "Whitney Leavitt", listOf("Whitney"))
    )

    internal fun questionCountForTest(topic: Topic): Int = fixedQuestions.count { it.topic == topic }

    fun next(topics: List<Topic>, recentIds: Collection<String> = emptyList()): Question {
        val chosenTopics = topics.ifEmpty { Topic.entries.toList() }
        if (Topic.MATHS in chosenTopics && (chosenTopics.size == 1 || Random.nextInt(100) < 45)) {
            return mathsQuestion()
        }
        val pool = fixedQuestions.filter { it.topic in chosenTopics && it.id !in recentIds }
            .ifEmpty { fixedQuestions.filter { it.topic in chosenTopics } }
            .ifEmpty { fixedQuestions }
        return pool.random()
    }

    fun isCorrect(question: Question, input: String): Boolean {
        val actual = normalise(input)
        val expected = listOf(question.answer) + question.acceptedAnswers
        if (expected.any { normalise(it) == actual }) return true

        val actualNumber = actual.replace(',', '.').toDoubleOrNull()
        val expectedNumber = question.answer.replace(',', '.').toDoubleOrNull()
        return actualNumber != null && expectedNumber != null && abs(actualNumber - expectedNumber) < 0.011
    }

    private fun normalise(value: String): String = value.trim().lowercase()
        .replace(Regex("[^a-z0-9.]+"), " ")
        .trim()

    private fun mathsQuestion(): Question {
        return when (Random.nextInt(4)) {
            0 -> {
                val a = Random.nextInt(0, 100)
                val b = Random.nextInt(0, 100)
                Question("math_${System.nanoTime()}", Topic.MATHS, "$a + $b = ?", (a + b).toString())
            }
            1 -> {
                val a = Random.nextInt(0, 100)
                val b = Random.nextInt(0, a + 1)
                Question("math_${System.nanoTime()}", Topic.MATHS, "$a − $b = ?", (a - b).toString())
            }
            2 -> {
                val a = Random.nextInt(2, 13)
                val b = Random.nextInt(2, 13)
                Question("math_${System.nanoTime()}", Topic.MATHS, "$a × $b = ?", (a * b).toString())
            }
            else -> {
                val divisor = Random.nextInt(2, 13)
                val quotient = Random.nextInt(2, 13)
                val dividend = divisor * quotient
                Question("math_${System.nanoTime()}", Topic.MATHS, "$dividend ÷ $divisor = ?", quotient.toString())
            }
        }
    }
}
