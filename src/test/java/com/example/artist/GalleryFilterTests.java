package com.example.artist;

import com.example.artist.entity.Artwork;
import com.example.artist.repository.ArtworkRepository;
import com.example.artist.service.*;
import com.example.artist.controller.GalleryFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class GalleryFilterTests {
    @Autowired ArtworkRepository repository;
    @Autowired GallerySearch search;
    @Autowired ArtworkTerms terms;
    @Autowired MockMvc mvc;
    private Artwork work(String title, int year, String material, boolean available, String price) {
        Artwork a = new Artwork(); a.setTitle(title); a.setTitleEn(title); a.setYear(year); a.setMaterial(material);
        a.setSurface("Папір"); a.setTechnique("Кисть"); a.setGenre("Квіти"); a.setAvailable(available);
        if(price!=null) a.setPrice(new BigDecimal(price));
        return repository.saveAndFlush(a);
    }
    @Test void filtersCombineYearMaterialSurfaceTechniqueGenreAvailabilityAndPrice() {
        Artwork match=work("Match",2025,"Акварель",true,"1500");
        work("Wrong material",2025,"Олія",true,"1500");
        work("Sold",2025,"Акварель",false,"1500");
        work("Wrong year",2024,"Акварель",true,"1500");
        GalleryFilter f=new GalleryFilter(); f.setYear(2025);f.setMaterial("Акварель");f.setSurface("Папір");
        f.setTechnique("Кисть");f.setGenre("Квіти");f.setAvailability("available");f.setPriceMin(new BigDecimal("1000"));f.setPriceMax(new BigDecimal("2000"));
        assertThat(search.search(f,Locale.ENGLISH)).extracting(Artwork::getId).containsExactly(match.getId());
    }
    @Test void sortingUsesSelectedCurrencyAndKeepsUnpricedWorksLast() {
        Artwork a=work("A",2026,"Акрил",true,"100");a.setPriceUsd(new BigDecimal("90")); repository.saveAndFlush(a);
        Artwork b=work("B",2026,"Акрил",true,"200");b.setPriceUsd(new BigDecimal("10")); repository.saveAndFlush(b);
        Artwork c=work("C",2026,"Акрил",true,null);
        GalleryFilter f=new GalleryFilter();f.setSort("priceAsc");f.setCurrency("USD");
        assertThat(search.search(f,Locale.ENGLISH)).extracting(Artwork::getId).containsExactly(b.getId(),a.getId(),c.getId());
        f.setSort("priceDesc");
        assertThat(search.search(f,Locale.ENGLISH)).extracting(Artwork::getId).containsExactly(a.getId(),b.getId(),c.getId());
    }
    @Test void draftsNeverAppearInSearchOrPublicFacetOptions() {
        Artwork draft=work("Secret",2026,"Secret material",true,"100"); draft.setPublished(false);repository.saveAndFlush(draft);
        assertThat(search.search(new GalleryFilter(),Locale.ENGLISH)).isEmpty();
        assertThat(terms.used("material",true)).doesNotContain("Secret material");
    }
    @Test void galleryRendersBothLanguagesAndRejectsInvalidPriceRange() throws Exception {
        work("Test",2026,"Акварель",true,"100");
        for(String lang:java.util.List.of("uk","en")) mvc.perform(get("/gallery").param("lang",lang).param("material","Акварель").param("year","2026"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("gallery-toolbar")))
            .andExpect(model().attribute("filterValues", org.hamcrest.Matchers.hasEntry(org.hamcrest.Matchers.equalTo("material"), org.hamcrest.Matchers.hasItem("Акварель"))))
            .andExpect(content().string(containsString("value=\"Акварель\"")));
        mvc.perform(get("/gallery").param("priceMin","200").param("priceMax","100").param("lang","en"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Check the price range")));
        mvc.perform(get("/gallery").param("year","bad")).andExpect(status().isOk());
    }
    @Test @WithMockUser(roles="ADMIN")
    void settingsAddTermsAndEditorUsesNewTermsWithoutLosingLegacyValues() throws Exception {
        Artwork old=work("Legacy",2020,"Legacy material",true,"100");
        mvc.perform(get("/admin/settings")).andExpect(status().isOk());
        mvc.perform(post("/admin/settings").with(csrf()).param("kind","technique").param("value","Лесування").param("english","Glazing"))
            .andExpect(redirectedUrl("/admin/settings?kind=technique"));
        assertThat(terms.label("technique","Лесування",Locale.ENGLISH)).isEqualTo("Glazing");
        mvc.perform(get("/admin/artworks/"+old.getId()+"/edit").param("lang","en")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Glazing"))).andExpect(content().string(containsString("Legacy material")));
    }
    @Test void settingsRequireAdministratorAndCsrf() throws Exception {
        mvc.perform(get("/admin/settings")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/settings")).andExpect(status().isForbidden());
    }
    @Test void yearAndTitleSortingAreStable() {
        Artwork a=work("Beta",2026,"Олія",true,"100");Artwork b=work("Alpha",2020,"Олія",false,"100");
        GalleryFilter f=new GalleryFilter(); f.setSort("oldest");
        assertThat(search.search(f,Locale.ENGLISH)).extracting(Artwork::getId).containsExactly(b.getId(),a.getId());
        f.setSort("title"); assertThat(search.search(f,Locale.ENGLISH)).extracting(Artwork::getId).containsExactly(b.getId(),a.getId());
        f.setAvailability("sold"); assertThat(search.search(f,Locale.ENGLISH)).extracting(Artwork::getId).containsExactly(b.getId());
    }
}
