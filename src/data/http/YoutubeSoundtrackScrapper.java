package data.http;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.entry.GameEntry;
import ui.Main;

import java.io.*;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ui.Main.DEV_MODE;

/**
 * Created by LM on 07/08/2016.
 */
public class YoutubeSoundtrackScrapper {
    private static String[] EXCLUDED_YOUTUBE_AUTHORS = new String[]{"GoldenApple","Shirikova","Lolnoobwut1","Swift Phantom IV","Hux Gaming OSTs","Adam Kenway","Vittles","IGN","NightShader1","PewDiePie","VanossGaming","Fernanfloo","KSI","Markiplier","Machinima","Sky Does Minecraft","TheDiamondMinecart // DanTDM","jacksepticeye","TheWillyrex","TheSyndicateProject","CaptainSparklez","PopularMMOs","Ali-A","rezendeevil","W2S","stampylonghead","HowToBasic","H2ODelirious","League of Legends","YOGSCAST Lewis &amp; Simon","Willyrex","Smosh Games","TobyGames","The Game Theorists","SSundee","speedyw03","SQUEEZIE","theRadBrad","aLexBY11","AuthenticGames","TazerCraft","Bajan Canadian - Minecraft &amp; More","SeaNanners Gaming Channel","VenomExtreme","FROST","luzugames","sTaXxCraft","Annoying Orange","Kwebbelkop","El Rincón De Giorgio","Clash of Clans","iHasCupquake","miniminter","PlayStation","Coisa de Nerd","Daithi De Nogla","TheBrainDit","Jelly","Lui Calibre","KSIOlajidebtHD","Call of Duty","LeafyIsHere","I AM WILDCAT","BRKsEDU","MrLololoshka (Роман…","Gronkh","JeromeASF","sTaXx","AM3NlC","FaZe Clan","CyprienGaming","MoreAliA","Gameplayrj","iBallisticSquid","LetsPlay","GameGrumps","videogames","Vikkstar123","TheAtlanticCraft | Minecraft","TheGamingLemon","Mini Ladd","DaniRep | +6 Vídeos Diarios De GTA…","SSSniperWolf","Typical Gamer","CasalDeNerd","PrestonPlayz - Minecraft","FRANKIEonPCin1080p","TmarTn","SirKazzio","Zangado","Fer0m0nas","Chris Smoove","ScrewAttack!","UberHaxorNova","➜FavijTV™","FaZe Rug","TheGrefg","M3RKMUS1C","DashieGames","Rockstar Games","Realm Games","Vilhena","OpTic Pamaj","JonTronShow","Joueur Du Grenier","Nadeshot","machinimarespawn","Slogoman","videogamedunkey","LittleLizardGaming - Minecraft…","CinnamonToastKen","XpertThief","Vikkstar123HD - Minecraft &amp; More","Lachlan","Calfreezy","Русский Мясник","AngryJoeShow","Scarce","Sarinha","Cryaotic","whiteboy7thst","HappyTown | LeTSPLaySHiK","Egoraptor","gameranx","OfficialNerdCubed","ArcadeGo.com","BasicallyIDoWrk","GameSpot","MR.HEART ROCKER","malena010102","BCC Trolling","Jazzghost","World of Tanks.…","VenturianTale","Jove [Virtus.Pro]","Zerkaa","LDShadowLady","DanAndPhilGAMES","ChrisMD","AntVenom","TBJZL","MasterOv Slither.io Pokemon Go","MM7Games","d7oomy_999 | دحومي٩٩٩","KYRSP33DY","MrPoladoful","TotalBiscuit, The Cynical Brit","ZackScottGames","EA SPORTS FIFA","HikakinGames","lele","DeadloxMC - Minecraft &amp; More","Kuplinov ► Play","Gona89","NoahJ456","reZigiusz","TerrorBionic","Cinemassacre","SuperEvgexa","SAH4R SHOW","Aphmau","YouAlwaysWin","XboxAddictionz","DILLERON ★ Play","Burak Oyunda","PietSmiet","MessYourself","swiftor","Blowek","Luiz1227","Chief Pat - Clash Royale &amp; Clash of…","LoL Esports","Siv HD","TaaFUCK","ungespielt","DidYouKnowGaming?","Scumperjumper","Moo Snuckel","GamingWithJen","Behzinga","Stuu Games","FGTeeV","FaZe Blaziken","lisbug","YOGSCAST Sjin","HikePlays","FaZe Jev","Ubisoft","Sarazar","Bodil40","SethBling","GermanLetsPlay","YOGSCAST Duncan","MattHDGamer","Joey Graceffa","Sips","EthosLab","funkyblackcat","ChimneySwift11","LipaoGamer","jvnq","Battlefield","NDNG - Enes Batur","ConCrafter | LUCA","Bashurverse","Lugin","ElChurches","Siphano","KilooGames","skgames","HuskyMUDKIPZ","viniccius13","jackfrags","ilvostrocaroDexter","Nintendo","WiiFer0iiz","AmbuPlay","Luh","outsidexbox","Diabl0x9","zbing z.","MrWoofless","PointlessBlogGames","Le Bled'Art","Spok","jahovaswitniss","양띵 YouTube (YD Gaming Channel)","Smike","Guava Juice","SUP3R KONAR !","Keralis","Sparkles ☆ #1 Gaming - CSGO &amp;…","Dota Watafak","HappyTown | LaGGeRFeeD","GhostRobo","Godson - Clash Royale &amp; Clash of…","BaixaMemoria","JugandoConNatalia","FantaBobGames","rewinside","Paluten","SYBO","MrLEV12","TheFantasio974","Xbox","TBNRfrags","GONTH","Thinknoodles","Terroriser","Ярик Лапа","Crainer Commentates","PeanutButterGamer","RedKeyMon","SkizzTV","Node","Amixem","Scott Cawthon","Josemicod5","penguinz0","Bay Riffer","ThatcherJoeGames","Filipin is bro","Smooth McGroove","TheDuckVoice","Febatista","Exetrize","LevelCapGaming","ChaBoyyHD","KillerCreeper55","KevinLaSean","Spencer FC","byViruZz","Oyun Delisi","MrUnfiny","RusGameTactics","TheJWittz","BenderChat","Frigiel","Prestige Clips","SkyVSGaming","Brofresco","PlayComedyClub","マックスむらい","Saudi Gamer - سعودي جيمر","GommeHD","John Morin","ElectronicDesireGE","GTA Series Videos","NobodyEpic","TheCommunityChannel","TheAlvaro845 - Clash Royale - Clash…","GAMINGwithMOLT","WaRTeKGaminG","TheDeluxe4","CiccioGamer89","SemchenkoKirill","Фирамир","Канал Кейна","TeamMojang","MrBossFTW","Felps","OnlineGamer","JMX","Typhoon Cinema","The Alex Super","DotaCinema","EugeneSagaz","IsAmUxPompa","MrDalekJD","yamimash","The Warp Zone","windy31","Robbaz","Аид [VyacheslavOO]","はじめしゃちょー2 (hajime)","paulsoaresjr","MxR Mods","Blitzwinger","YOGSCAST Hannah","ZerkaaPlays","HelldogMadness","DaniRepHappy Minecraft a tope!","GoldGloveTV","The Official Pokémon Channel","Patife","AlphaSniper97","DownRangeGaming","TetraNinja","CaRtOoNz","Samgladiator","NepentheZ","Dane Boe","Instalok","Aypierre","OpTic | 2016","GameSprout","chaosxsilencer","Folagor03","SurreaIPower","YOGSCAST Martyn","Robin Hood Gamer","Hueva","Anomaly","赤髪のとものゲーム実況チャンネル!!…","stacyplays","AviatorGaming","maxmoefoegames","Quantum Games","SSoHPKC","Cam Sucks at Gaming","WhaTheGame","ROJSON","MagmaMusen","Fir4sGamer","Team Epiphany - GTA 5 Mods","LispyJimmy","Luna","CriousGamers (Chilled Chaos)","대도서관TV (buzzbean11)","perxitaa","Vietnam Esports TV","MattShea","MuratAbiGF","兄者弟者","Pyrocynical","packattack04082","Xodaaaa","Mandzio","Gizzy Gazza","MrBboy45","TheSkylanderBoy AndGirl","Galadon Gaming - Clash and Pokemon…","Drift0r","TWOSYNC","SkinSpotlights","alexelcapo","Montalvaao","Beh2inga","Planet Dolan GAMING","ImmortalHD","Ryan McNulty - My Mom Says I'm…","Amway921WOT","E IL o T IR i X ™","NGTZombies","LaSalle","TheSmithPlays","nickatnyte","PlayStation Access","Coffi Channel","VintageBeef","TheRPGMinx","Na`Vi.Dota 2","HurderOfBuffalo","iijeriichoii","TheRelaxingEnd","LPmitKev","RobTopGames","MissPinguina","Журнал","Игромания","Fangs","gagatunFeed","MADFINGER Games a.s.","consolesejogosbrasil","TeamGarryMovieThai","Mumbo Jumbo","Grian","BdoubleO100","MYSTIC7 Gaming","AA9skillz","AN7HONY96","Mr. Marmok","Logdotzip","Шурик | ShurikWorld","TeranitGame","Controle Dois","JustSnake","MULTI","AnEsonGib","TheSLAPTrain","Remigiusz Maciaszek","GOTAGA","SpeirsTheAmazingHD","Zombey","PlayHard - Clash of Clans","Guilherme GAMER","Games EduUu","Protatomonster","TaGs - Play Theme","Bulkin","SoCloseToToast","Jaidefinichon GOTH","First Person Troller","Cronosplays","Mr Sark","cobanermani456","1337LikeR","MayconLorenz","izak LIVE","chuggaaconroy","UberDanger","EthanGamerTV / EGTV","Juxiis","R Λ Z Ξ R","악어 유튜브","Demaster","iCrazyTeddy","Sp4zie","Achievement Hunter","Rabahrex","MarkOfJ","Sl1pg8r - Daily Stuff and Things!","AlexPozitiv","Dat Saintsfan","ItsJerryAndHarry","도티 TV","SideArms4Reason","Makiman131","J0k3ROfficialTube","Skyrroz","Inemafoo","BlackSilverUFA","JackFrostMiner","Karolek","Generikb","AJ3","Vardoc1","Nightblue3","PrestigeIsKey","Canal do Pigman","Olli43","CapgunTom","Draegast","TOM SHUFFLE","LunaDangelis","Oyun Portal","Herr Bergmann","ZaziNombies LEGO Creations","Bboymoreno92 ¿Quieres leche? Super…","TCTNGaming","CooLifeGame","Redmercy","LordMinion777","creative7play","Minikotic ★ Play","Nick Bunyun","kootra","TGN","ThnxCya","TheIvaneh","Gimper","Furious Jumper","Dmitriy Landstop","KOSDFF tK","LubaTV Games","italo matheus","TheDaarick28","KhaleDQ84EveR","MagicAnimalClub","OlafVids","Xcrosz","Manny","Mr.Freeze ►","Assassin's Creed","AdamsonShow","Valve"};
    private static String[] PREFERRED_YOUTUBE_AUTHORS = new String[]{"Epic Video Game Music","Człowiek Drzewo"};
    private final static String[] SOUNDTRACK_KEY_WORDS = new String[]{"game main theme", "game main menu music", "game soundtrack"};
    private final static String[] ESSENTIAL_KEY_WORDS = new String[]{"theme", "music", "soundtrack", "soundtrack", "ost", "song", "melody"};

    private final static String VIDEO_SEARCH_SEPARATOR =  "<button class=\"yt-uix-button yt-uix-button-size-small yt-uix-button-default yt-uix-button-empty yt-uix-button-has-icon no-icon-markup addto-button video-actions spf-nolink hide-until-delayloaded addto-watch-later-button-sign-in yt-uix-tooltip\" type=\"button\" onclick=\";return false;\"";
    private final static String VIDEO_URL_PREFIX = "<h3 class=\"yt-lockup-title \"><a href=\"/watch?v=";
    private final static String VIDEO_TITLE_PREFIX = "\" title=\"";
    private final static String VIDEO_AUTHOR_SUFFIX = "</a></div><div class=\"yt-lockup-meta\"><ul class=\"yt-lockup-meta-info\"><li>";
    private final static String VIDEO_AUTHOR_VALIDATED_SUFFIX = "</a>&nbsp;<span class=\"yt-uix-tooltip yt-channel-title-icon-verified yt-sprite\"";

    public static String getThemeYoutubeHash(GameEntry entry) throws IOException, UnirestException {
        ArrayList<VideoMetadata> videoMetadatas = new ArrayList<>();
        for(String keywords : SOUNDTRACK_KEY_WORDS){
            ArrayList<VideoMetadata> searchResults = getVideosTitlesAndLinksFor(entry.getName(),keywords);
            ArrayList<VideoMetadata> toAdd = new ArrayList<>();
            for(VideoMetadata found : searchResults){
                boolean addIt = true;
                for(VideoMetadata alreadyFound : videoMetadatas){
                    addIt = !found.getHash().equals(alreadyFound.getHash());
                    if(!addIt){
                        break;
                    }
                }
                if(addIt){
                    toAdd.add(found);
                }
            }
            videoMetadatas.addAll(toAdd);
        }
        rankSoundtrackResults(videoMetadatas, entry.getName());
        Main.LOGGER.info("Using soundtrack : "+videoMetadatas.get(0));
        return videoMetadatas.get(0).hash;

    }
    private static void rankSoundtrackResults(ArrayList<VideoMetadata> videoMetadatas, String gameName){
        HashMap<String, Integer> pointsForKeyword = new HashMap<>();

        int pointForExcludedAuthor = -100;
        int pointForPreferredAuthor = 25;
        pointsForKeyword.put("soundtrack", 5);
        pointsForKeyword.put("ost", 5);
        pointsForKeyword.put("main theme",20);
        pointsForKeyword.put("official", 3);
        pointsForKeyword.put("menu", 8);
        pointsForKeyword.put("main menu", 30);
        pointsForKeyword.put("music",5);
        pointsForKeyword.put("remix",-50);
        pointsForKeyword.put("edit",-30);
        pointsForKeyword.put("cover", -20);
        pointsForKeyword.put("Note Block",-30);
        pointsForKeyword.put("NoteBlock",-30);
        pointsForKeyword.put("Game of Thrones",-100);
        pointsForKeyword.put("piano",-20);
        pointsForKeyword.put("guitar",-20);
        pointsForKeyword.put("song",1);
        pointsForKeyword.put("orchestra",1);
        pointsForKeyword.put("Funny moments",-100);
        pointsForKeyword.put("Ep.",-20);
        pointsForKeyword.put("Episode",-50);
        pointsForKeyword.put("Full",-15);
        pointsForKeyword.put("MIDI",-35);
        pointsForKeyword.put("Synthesia",-35);
        pointsForKeyword.put("bug",-50);
        pointsForKeyword.put("Transcription",-15);
        pointsForKeyword.put("Gameplay",-50);
        pointsForKeyword.put("Earrape",-100);
        pointsForKeyword.put("Glitch",-100);

        for(int i=0; i<videoMetadatas.size(); i++){
            VideoMetadata metadata = videoMetadatas.get(i);
            if(!cleanTitle(metadata.getTitle()).contains(cleanTitle(gameName))){
                metadata.points-=100;
            }
            if(!cleanTitle(metadata.getTitle()).contains(cleanTitle(gameName))){
                metadata.points-=100;
            }
            if(cleanTitle(metadata.getTitle()).contains(cleanTitle(gameName+" "))){
                metadata.points+=50;
            }
            boolean none = false;
            for(String keyword : ESSENTIAL_KEY_WORDS){
                none = !cleanTitle(metadata.getTitle()).contains(cleanTitle(keyword));
                if(!none){
                    break;
                }
            }
            if(none){
                metadata.points-=100;
            }
            for(String keyword : pointsForKeyword.keySet()){
                if(cleanTitle(metadata.getTitle()).contains(cleanTitle(keyword))){
                    metadata.points+=pointsForKeyword.get(keyword);
                }
            }
            if(!metadata.getAuthor().equals("")) {
                for (String author : EXCLUDED_YOUTUBE_AUTHORS) {
                    if (metadata.getAuthor().toLowerCase().contains(author.toLowerCase())){
                        metadata.points+=pointForExcludedAuthor;
                    }
                }
                for (String author : PREFERRED_YOUTUBE_AUTHORS) {
                    if (metadata.getAuthor().toLowerCase().contains(author.toLowerCase())){
                        metadata.points+=pointForPreferredAuthor;
                    }
                }
            }
            Pattern pattern = Pattern.compile(cleanTitle(gameName)+".{0,13}(theme|menu|main).{1,6}(theme|menu|soundtrack|music|song)");
            Matcher matcher = pattern.matcher(cleanTitle(metadata.title));
            if(matcher.find()){
                metadata.points+=100;
            }
        }
        videoMetadatas.sort(new Comparator<VideoMetadata>() {
            @Override
            public int compare(VideoMetadata o1, VideoMetadata o2) {
                return (o1.points > o2.points) ? -1 : 1;
            }
        });
        if(DEV_MODE){
            Main.LOGGER.debug("Found videos for game "+gameName+" : ");
            for(VideoMetadata m : videoMetadatas){
                Main.LOGGER.debug("\t"+m);
            }
        }
    }
    private static String cleanTitle(String gameTitle){
        return gameTitle.toLowerCase()
                .replace('-',' ')
                .replace('_',' ')
                .replace(':',' ')
                .replace('.',' ')
                .replace(',',' ')
                .replace('\'',' ')
                .replace('!',' ')
                .replace("  "," ")
                .replace("  "," ");
    }
    private static ArrayList<VideoMetadata> getVideosTitlesAndLinksFor(String gameName,String otherKeyWords) throws UnirestException, IOException {
        HttpResponse<InputStream> response = Unirest.get("https://www.youtube.com/results?search_query="+(gameName+" "+otherKeyWords).replace(' ','+'))
                .header("Content-Type", "text/html; charset=utf-8")
                .header("Accept", "text/html; charset=utf-8")
                .header("Accept-Charset","utf-8")
                .asBinary();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));

        boolean waitFor2ndLine = false;
        int lineAfterSeparator = 0;
        String line = null;
        ArrayList<VideoMetadata> result = new ArrayList();
        while((line = reader.readLine())!=null){
            if(waitFor2ndLine && lineAfterSeparator == 2){
                waitFor2ndLine =false;
                lineAfterSeparator = 0;

                //ON A VIDEO LINE!
                String subString =  line.substring(line.indexOf(VIDEO_URL_PREFIX)+VIDEO_URL_PREFIX.length());
                String hash = subString.substring(0,subString.indexOf('\"'));

                subString = subString.substring(subString.indexOf(VIDEO_TITLE_PREFIX)+VIDEO_TITLE_PREFIX.length());
                String title = subString.substring(0,subString.indexOf("\""));

                int suffixIndex = subString.indexOf(VIDEO_AUTHOR_SUFFIX);
                if(suffixIndex == -1){
                    suffixIndex = subString.indexOf(VIDEO_AUTHOR_VALIDATED_SUFFIX);
                }
                String author = "";
                for(int i = suffixIndex-1; i>0; i--){
                    if(subString.charAt(i)!='>'){
                        author=subString.charAt(i)+author;
                    }else{
                        break;
                    }
                }
                VideoMetadata metadata = new VideoMetadata();
                metadata.setHash(HTMLtoUTF8(hash));
                metadata.setTitle(HTMLtoUTF8(title));
                metadata.setAuthor(HTMLtoUTF8(author));
                result.add(metadata);

            }else if (waitFor2ndLine && lineAfterSeparator!=2){
                lineAfterSeparator++;
            }else{
                if(line.contains(VIDEO_SEARCH_SEPARATOR)){
                    waitFor2ndLine = true;
                    lineAfterSeparator++;
                }
            }

        }
        reader.close();
        return result;
    }
    public static String HTMLtoUTF8(String htmlText){
        return  org.apache.commons.lang.StringEscapeUtils.unescapeHtml(htmlText);
    }

    public static void main (String[] args) throws CharacterCodingException {
        String falseString = "Dark Souls III Soundtrack OST - Main Menu Theme";
        String correctString = "Besiege Menu's Theme Song";

        Pattern pattern = Pattern.compile(cleanTitle("Besiege")+".{0,13}(theme|menu|main).{1,6}(theme|menu|soundtrack|music|song)");
        Matcher matcher = pattern.matcher(cleanTitle(correctString));
        System.out.print("Cleaned : "+cleanTitle(correctString));
        if(matcher.find()){
            System.out.print("found");
        }

    }

    public static class VideoMetadata{
        private String hash ="";
        private String title="";
        private String author="";
        private int points=0;

        VideoMetadata(){}

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        @Override
        public String toString(){
            return "[hash="+ hash +"],[title="+title+"],[author="+author+"]"+",[points="+points+"]";
        }
    }
}
