/*
 * Copyright (c) 2002-2025, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.captcha.modules.jcaptcha.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.ImageFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.jhlabs.image.PinchFilter;
import com.octo.captcha.CaptchaFactory;
import com.octo.captcha.component.image.backgroundgenerator.GradientBackgroundGenerator;
import com.octo.captcha.component.image.color.SingleColorGenerator;
import com.octo.captcha.component.image.deformation.ImageDeformationByFilters;
import com.octo.captcha.component.image.fontgenerator.RandomFontGenerator;
import com.octo.captcha.component.image.textpaster.DecoratedRandomTextPaster;
import com.octo.captcha.component.image.textpaster.textdecorator.TextDecorator;
import com.octo.captcha.component.image.wordtoimage.DeformedComposedWordToImage;
import com.octo.captcha.component.word.wordgenerator.RandomWordGenerator;
import com.octo.captcha.engine.GenericCaptchaEngine;
import com.octo.captcha.engine.bufferedengine.ContainerConfiguration;
import com.octo.captcha.engine.bufferedengine.SimpleBufferedEngineContainer;
import com.octo.captcha.engine.bufferedengine.buffer.DiskCaptchaBuffer;
import com.octo.captcha.engine.bufferedengine.buffer.MemoryCaptchaBuffer;

import fr.paris.lutece.plugins.captcha.modules.jcaptcha.service.engine.image.filter.LuteceWaterFilter;
import fr.paris.lutece.plugins.captcha.modules.jcaptcha.service.engine.image.imagedeformation.LuteceImageDeformationByFilters;
import fr.paris.lutece.plugins.captcha.modules.jcaptcha.service.image.LuteceGimpyImageFactory;
import fr.paris.lutece.plugins.captcha.modules.jcaptcha.service.sound.LuteceBackgroundSoundMixerConfigurator;
import fr.paris.lutece.plugins.captcha.modules.jcaptcha.service.sound.LuteceGimpySoundFactory;
import fr.paris.lutece.plugins.captcha.modules.jcaptcha.service.sound.LuteceSoundConfigurator;
import fr.paris.lutece.plugins.captcha.modules.jcaptcha.service.sound.LuteceWordToSound;
import fr.paris.lutece.plugins.captcha.modules.jcaptcha.service.sound.filter.EchoFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@ApplicationScoped
public class JCaptchaProducer
{

    public RandomFontGenerator buildLutecerandomUnifontGen( )
    {
        Font [ ] fonts = new Font [ 3];
        fonts [0] = new Font( "nyala", 1, 50 );
        fonts [1] = new Font( "Bell MT", 0, 50 );
        fonts [2] = new Font( "Credit valley", 1, 50 );
        return new RandomFontGenerator( 50, 50, fonts );
    }

    public DeformedComposedWordToImage buildLuteceWordToImage( )
    {
        List<ImageFilter> imageFilters = new ArrayList<ImageFilter>( );
        imageFilters.add( new PinchFilter( ) );

        List<ImageFilter> waterFilters = new ArrayList<ImageFilter>( );
        waterFilters.add( new LuteceWaterFilter( 3, true, 20, 70 ) );

        return new DeformedComposedWordToImage( buildLutecerandomUnifontGen( ),
                new GradientBackgroundGenerator( 300, 100, Color.BLACK, new Color( 211, 211, 211 ) ),
                new DecoratedRandomTextPaster( 6, 7, new SingleColorGenerator( Color.WHITE ), new TextDecorator [ ] { } ),
                new ImageDeformationByFilters( null ), new LuteceImageDeformationByFilters( imageFilters ),
                new LuteceImageDeformationByFilters( waterFilters ) );
    }

    public LuteceWordToSound buildLuteceWordToSound( )
    {
        LuteceSoundConfigurator configurator = new LuteceSoundConfigurator( 10, 22050, 50 );

        LuteceBackgroundSoundMixerConfigurator mixerConfigurator = new LuteceBackgroundSoundMixerConfigurator( 50, new String [ ] {
                "background_0", "background_1"
        } );

        LuteceWordToSound result = new LuteceWordToSound( configurator, 4, 7, 1, 3, mixerConfigurator, new EchoFilter( 250, 10 ) );
        return result;
    }

    public RandomWordGenerator buildWordgen( )
    {
        return new RandomWordGenerator( "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" );
    }

    public GenericCaptchaEngine buildImageEngine( )
    {
        return new GenericCaptchaEngine( new CaptchaFactory [ ] {
                new LuteceGimpyImageFactory( buildWordgen( ), buildLuteceWordToImage( ) )
        } );
    }

    public GenericCaptchaEngine buildSoundEngine( )
    {
        return new GenericCaptchaEngine( new CaptchaFactory [ ] {
                new LuteceGimpySoundFactory( buildWordgen( ), buildLuteceWordToSound( ) )
        } );
    }

    @Produces
    @Singleton
    @Named( JCaptchaEngineService.BEAN_NAME_JCAPTCHA_IMAGE_SERVICE )
    public SimpleBufferedManageableCaptchaService produceImageCaptchaService( )
    {
        GenericCaptchaEngine engine = buildImageEngine( );

        SimpleBufferedEngineContainer container = new SimpleBufferedEngineContainer( engine, new MemoryCaptchaBuffer( ), new DiskCaptchaBuffer( "", false ),
                new ContainerConfiguration( new HashMap( ), 100, 100, 10, 5 ), 10000, 1000 );

        SimpleBufferedManageableCaptchaService result = new SimpleBufferedManageableCaptchaService( engine, container, 180, 200 );

        return result;
    }

    @Produces
    @Singleton
    @Named( JCaptchaEngineService.BEAN_NAME_JCAPTCHA_SOUND_SERVICE )
    public SimpleBufferedManageableCaptchaService produceSoundCaptchaService( )
    {
        GenericCaptchaEngine engine = buildSoundEngine( );

        SimpleBufferedEngineContainer container = new SimpleBufferedEngineContainer( engine, new MemoryCaptchaBuffer( ), new DiskCaptchaBuffer( "", false ),
                new ContainerConfiguration( new HashMap( ), 100, 100, 10, 5 ), 10000, 1000 );

        SimpleBufferedManageableCaptchaService result = new SimpleBufferedManageableCaptchaService( engine, container, 180, 200 );

        return result;
    }

}
